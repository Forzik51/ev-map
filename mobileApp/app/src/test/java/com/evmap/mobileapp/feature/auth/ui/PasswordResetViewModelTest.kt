package com.evmap.mobileapp.feature.auth.ui

import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.ForgotPasswordResponse
import com.evmap.mobileapp.core.data.dto.VerifyPasswordCodeResponse
import com.evmap.mobileapp.feature.auth.data.PasswordResetFailure
import com.evmap.mobileapp.feature.auth.data.PasswordResetGateway
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var gateway: FakeGateway
    private lateinit var vm: PasswordResetViewModel

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        gateway = FakeGateway()
        vm = PasswordResetViewModel(gateway)
    }
    @After fun cleanup() { vm.viewModelScope.cancel(); Dispatchers.resetMain() }

    @Test fun `recovery completes only after server confirmation and clears secrets`() = runTest(dispatcher) {
        requestCode()
        assertEquals(PasswordResetStep.CODE, vm.state.value.step)
        vm.updateCode("12345678"); vm.verifyCode(); runCurrent()
        assertEquals(PasswordResetStep.PASSWORD, vm.state.value.step)
        assertEquals("", vm.state.value.code)
        vm.updatePassword("new password"); vm.updateConfirmation("new password")
        gateway.resetPending = CompletableDeferred()
        vm.savePassword(); runCurrent()
        assertTrue(vm.state.value.loading)
        assertEquals(PasswordResetStep.PASSWORD, vm.state.value.step)
        gateway.resetPending!!.complete(Result.success(Unit)); runCurrent()
        assertEquals(PasswordResetStep.SUCCESS, vm.state.value.step)
        assertEquals("", vm.state.value.password)
        assertEquals("", vm.state.value.confirmation)
        assertEquals("reset-grant" to "new password", gateway.saved)
    }

    @Test fun `bad email and duplicate taps never issue extra requests`() = runTest(dispatcher) {
        vm.updateEmail("invalid"); vm.sendCode(); runCurrent()
        assertEquals(0, gateway.requests)
        assertNotNull(vm.state.value.error)
        vm.updateEmail(" person@example.com "); vm.sendCode(); vm.sendCode(); runCurrent()
        assertEquals(1, gateway.requests)
        assertEquals("person@example.com", vm.state.value.email)
    }

    @Test fun `resend waits for cooldown and uses the new request id`() = runTest(dispatcher) {
        requestCode()
        vm.sendCode(); runCurrent(); assertEquals(1, gateway.requests)
        advanceTimeBy(60_000); runCurrent()
        vm.sendCode(); runCurrent(); assertEquals(2, gateway.requests)
        vm.updateCode("12345678"); vm.verifyCode(); runCurrent()
        assertEquals("request-2", gateway.verifiedRequest)
    }

    @Test fun `wrong code stays on code screen`() = runTest(dispatcher) {
        requestCode()
        gateway.verifyResult = Result.failure(PasswordResetFailure("INVALID_OR_EXPIRED_CODE", null, "Invalid code"))
        vm.updateCode("12345678"); vm.verifyCode(); runCurrent()
        assertEquals(PasswordResetStep.CODE, vm.state.value.step)
        assertEquals("Invalid code", vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    @Test fun `password confirmation and UTF8 byte limit are validated before API call`() = runTest(dispatcher) {
        requestCode(); vm.updateCode("12345678"); vm.verifyCode(); runCurrent()
        vm.updatePassword("new password"); vm.updateConfirmation("different"); vm.savePassword(); runCurrent()
        assertNull(gateway.saved)
        vm.updatePassword("я".repeat(37)); vm.updateConfirmation("я".repeat(37)); vm.savePassword(); runCurrent()
        assertNull(gateway.saved)
        assertNotNull(vm.state.value.error)
    }

    @Test fun `expired grant requires recovery again and removes passwords`() = runTest(dispatcher) {
        requestCode(); vm.updateCode("12345678"); vm.verifyCode(); runCurrent()
        gateway.resetResult = Result.failure(PasswordResetFailure("INVALID_OR_EXPIRED_RESET_TOKEN", null, "Start again"))
        vm.updatePassword("new password"); vm.updateConfirmation("new password"); vm.savePassword(); runCurrent()
        assertEquals(PasswordResetStep.EMAIL, vm.state.value.step)
        assertEquals("", vm.state.value.password)
        assertEquals("Start again", vm.state.value.error)
    }

    @Test fun `rate limit preserves server retry delay`() = runTest(dispatcher) {
        gateway.requestError = PasswordResetFailure("RESET_RATE_LIMITED", 120, "Wait")
        vm.updateEmail("person@example.com"); vm.sendCode(); runCurrent()
        assertEquals(120L, vm.state.value.resendSeconds)
        vm.sendCode(); runCurrent(); assertEquals(1, gateway.requests)
    }

    @Test fun `leaving pending verification prevents late transition`() = runTest(dispatcher) {
        requestCode()
        gateway.verifyPending = CompletableDeferred()
        vm.updateCode("12345678"); vm.verifyCode(); runCurrent()
        vm.restart()
        gateway.verifyPending!!.complete(Result.success(VerifyPasswordCodeResponse("reset-grant", 300)))
        runCurrent()
        assertEquals(PasswordResetStep.EMAIL, vm.state.value.step)
        assertFalse(vm.state.value.loading)
    }

    private fun TestScope.requestCode() {
        vm.updateEmail("person@example.com"); vm.sendCode(); runCurrent()
    }

    private class FakeGateway : PasswordResetGateway {
        var requests = 0
        var requestError: Throwable? = null
        var verifiedRequest: String? = null
        var saved: Pair<String, String>? = null
        var verifyResult = Result.success(VerifyPasswordCodeResponse("reset-grant", 300))
        var resetResult = Result.success(Unit)
        var verifyPending: CompletableDeferred<Result<VerifyPasswordCodeResponse>>? = null
        var resetPending: CompletableDeferred<Result<Unit>>? = null
        override suspend fun requestCode(email: String): Result<ForgotPasswordResponse> {
            requests++
            return requestError?.let { Result.failure(it) }
                ?: Result.success(ForgotPasswordResponse("request-$requests", 60, "Accepted"))
        }
        override suspend fun verifyCode(requestId: String, code: String): Result<VerifyPasswordCodeResponse> {
            verifiedRequest = requestId
            return verifyPending?.await() ?: verifyResult
        }
        override suspend fun resetPassword(token: String, password: String): Result<Unit> {
            saved = token to password
            return resetPending?.await() ?: resetResult
        }
    }
}
