package com.evmap.serverapp.features.user.api

import com.evmap.serverapp.features.auth.PasswordResetIntegrationTest
import com.evmap.serverapp.features.user.application.command.UserPhotoService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.support.TransactionTemplate
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import javax.imageio.ImageIO

@EnabledIfEnvironmentVariable(named = "EVMAP_TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserProfileIntegrationTest {
    @LocalServerPort private var port = 0
    @Autowired private lateinit var db: DSLContext
    @Autowired private lateinit var json: ObjectMapper
    @Autowired private lateinit var encoder: PasswordEncoder
    @Autowired private lateinit var photos: UserPhotoService
    @Autowired private lateinit var transactions: TransactionTemplate

    private val client = HttpClient.newHttpClient()
    private val password = "profile test password"

    companion object {
        @TempDir @JvmField var mediaRoot: Path? = null

        @JvmStatic @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            PasswordResetIntegrationTest.database(registry)
            registry.add("app.media.root") { checkNotNull(mediaRoot).toString() }
        }
    }

    @Test
    fun `profile updates all editable fields and the renamed user can sign in`() {
        val user = account()
        val username = "New-${UUID.randomUUID().toString().take(10)}"
        val phone = randomPhone()
        val fields = mapOf("name" to "Олена", "surname" to "Коваль", "username" to username, "phone" to phone, "pageDescription" to "About me")

        assertEquals(401, request("PATCH", "/api/users/me", payload = fields).statusCode())
        val updated = body(request("PATCH", "/api/users/me", user.token, fields), 200)
        for ((field, value) in fields) assertEquals(value, updated[field].asText())
        assertEquals(user.id, updated["id"].asLong())
        assertEquals(user.email, updated["email"].asText())
        assertEquals(user.id, body(login(username.lowercase()), 200)["userId"].asLong())
        assertEquals(updated, body(request("GET", "/api/users/me", user.token), 200))

        val cleared = fields + mapOf("surname" to null, "pageDescription" to "")
        val result = body(request("PATCH", "/api/users/me", user.token, cleared), 200)
        assertTrue(result["surname"].isNull)
        assertEquals("", result["pageDescription"].asText())
    }

    @Test
    fun `validation and identity conflicts are field specific and leave profile unchanged`() {
        val user = account()
        val other = account(shortEmail = true)
        val fields = mapOf("name" to "Changed", "surname" to null, "username" to user.username, "phone" to user.phone, "pageDescription" to "Changed")
        val before = body(request("GET", "/api/users/me", user.token), 200)

        for ((field, value) in listOf("username" to other.username.uppercase(), "username" to other.email, "phone" to other.phone)) {
            val conflict = body(request("PATCH", "/api/users/me", user.token, fields + (field to value)), 409)
            assertTrue(conflict["fieldErrors"][field].asText().isNotBlank())
        }
        val invalid = body(request("PATCH", "/api/users/me", user.token, fields + ("phone" to "invalid")), 400)
        assertTrue(invalid["fieldErrors"]["phone"].asText().isNotBlank())
        assertEquals(before, body(request("GET", "/api/users/me", user.token), 200))
    }

    @Test
    fun `avatar changes check ownership preserve files on rollback and update follow lists`() {
        val user = account()
        val follower = account()
        val photoUrl = "/api/users/${user.id}/photo"
        val png = ByteArrayOutputStream().apply { ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", this) }.toByteArray()
        assertEquals(403, upload(photoUrl, follower.token, png).statusCode())
        val originalPath = body(upload(photoUrl, user.token, png), 200)["path"].asText()
        val originalFile = mediaFile(originalPath)
        assertTrue(Files.isRegularFile(originalFile))

        val previousContext = SecurityContextHolder.getContext()
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext().apply {
            authentication = UsernamePasswordAuthenticationToken.authenticated(user.id.toString(), "", emptyList())
        })
        val rolledBackFile = try {
            transactions.execute { status ->
                val path = photos.replace(user.id, MockMultipartFile("file", "photo.png", "image/png", png)).path
                val file = mediaFile(path)
                assertTrue(Files.isRegularFile(originalFile))
                assertTrue(Files.isRegularFile(file))
                status.setRollbackOnly()
                file
            }!!
        } finally {
            SecurityContextHolder.setContext(previousContext)
        }
        assertTrue(Files.isRegularFile(originalFile))
        assertFalse(Files.exists(rolledBackFile))
        assertEquals(originalPath, body(request("GET", photoUrl, user.token), 200)["path"].asText())

        assertEquals(204, request("POST", "/api/users/${follower.id}/follow/${user.id}", follower.token).statusCode())
        val newPath = body(upload(photoUrl, user.token, png), 200)["path"].asText()
        assertFalse(Files.exists(originalFile))
        val following = body(request("GET", "/api/users/${follower.id}/following", follower.token), 200)
        assertEquals(newPath, following.single()["path"].asText())
        assertEquals(403, request("DELETE", photoUrl, follower.token).statusCode())
        repeat(2) { assertEquals(204, request("DELETE", photoUrl, user.token).statusCode()) }
        assertFalse(Files.exists(mediaFile(newPath)))
        assertEquals(404, request("GET", photoUrl, user.token).statusCode())
        assertTrue(body(request("GET", "/api/users/${follower.id}/following", follower.token), 200).single()["path"].isNull)
    }

    private data class Account(val id: Long, val username: String, val phone: String, val email: String, val token: String)

    private fun account(shortEmail: Boolean = false): Account {
        val key = UUID.randomUUID().toString().replace("-", "").take(12)
        val username = "user-$key"
        val email = if (shortEmail) "$key@ex.com" else "$key@example.com"
        val phone = randomPhone()
        db.execute(
            """INSERT INTO app_user(name, email, phone, birthdate, password_hash, username, page_description)
               VALUES ('Test', ?, ?, DATE '1990-01-01', ?, ?, '')""",
            email, phone, encoder.encode(password), username,
        )
        val auth = body(login(email), 200)
        return Account(auth["userId"].asLong(), username, phone, email, auth["accessToken"].asText())
    }

    private fun randomPhone() = "+${ThreadLocalRandom.current().nextLong(100_000_000_000L, 1_000_000_000_000L)}"

    private fun login(identifier: String) = request(
        "POST", "/api/auth/login", payload = mapOf("usernameOrEmail" to identifier, "password" to password),
    )

    private fun mediaFile(path: String) = checkNotNull(mediaRoot).resolve(path.removePrefix("/media/"))

    private fun request(method: String, path: String, token: String? = null, payload: Any? = null): HttpResponse<ByteArray> {
        val builder = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path"))
        token?.let { builder.header("Authorization", "Bearer $it") }
        val data = if (payload == null) HttpRequest.BodyPublishers.noBody() else {
            builder.header("Content-Type", "application/json")
            HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(payload))
        }
        return client.send(builder.method(method, data).build(), HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun upload(path: String, token: String, bytes: ByteArray): HttpResponse<ByteArray> {
        val boundary = UUID.randomUUID().toString()
        val header = "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo.png\"\r\nContent-Type: image/png\r\n\r\n".encodeToByteArray()
        val footer = "\r\n--$boundary--\r\n".encodeToByteArray()
        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path"))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .PUT(HttpRequest.BodyPublishers.ofByteArray(header + bytes + footer)).build()
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun body(response: HttpResponse<ByteArray>, status: Int): JsonNode {
        assertEquals(status, response.statusCode(), response.body().decodeToString())
        return json.readTree(response.body())
    }
}
