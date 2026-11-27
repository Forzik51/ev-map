package com.evmap.serverapp.features.event.api.controller

import com.evmap.serverapp.features.event.application.normalizedTag
import com.evmap.serverapp.shared.api.FormValidationException
import com.evmap.serverapp.shared.security.CurrentUserProvider
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.util.UUID

data class CategoryProposal(val name: String)
data class CategoryDecision(val approved: Boolean,val reason: String? = null)

@RestController
@RequestMapping("/api/event-taxonomy")
class EventTaxonomyController(private val dsl: DSLContext,private val user: CurrentUserProvider,private val taxonomy: EventTaxonomyService) {
    @GetMapping("/tags") fun tags(@RequestParam(defaultValue="") query: String) = dsl.fetch("SELECT id,name FROM tag WHERE name ILIKE ? ORDER BY name LIMIT 30","%${query.take(30)}%").intoMaps()
    @PostMapping("/category-requests") fun propose(@RequestBody request: CategoryProposal) = taxonomy.propose(request.name,user.get().id)
    @GetMapping("/category-requests") fun requests(@RequestParam(defaultValue="false") all: Boolean): List<Map<String,Any>> {
        val where=if(all) { user.requireAdmin(); "" } else "WHERE user_id=${user.get().id}"
        return dsl.fetch("SELECT id,name,status,reason FROM category_request $where ORDER BY id DESC LIMIT 100").intoMaps()
    }
    @PostMapping("/category-requests/{id}/decision") fun decide(@PathVariable id: Long,@RequestBody decision: CategoryDecision) { user.requireAdmin();taxonomy.decide(id,decision) }
}

@Service
class EventTaxonomyService(private val dsl: DSLContext) {
    @Transactional fun propose(raw: String,userId: Long): Map<String,Any> {
        val name=raw.trim()
        if(name.length !in 1..60) throw FormValidationException(mapOf("categoryName" to "Назва категорії: від 1 до 60 символів"))
        val normalized=normalizedTag(name)
        if(dsl.fetchOne("SELECT 1 FROM category WHERE lower(name)=lower(?)",name)!=null) throw FormValidationException(mapOf("categoryName" to "Така категорія вже є у списку"))
        val row=dsl.fetchOne("INSERT INTO category_request(name,normalized_name,user_id) VALUES(?,?,?) ON CONFLICT(normalized_name) WHERE status='PENDING' DO NOTHING RETURNING id,name,status",name,normalized,userId)
        return row?.intoMap() ?: mapOf("status" to "PENDING","name" to name)
    }
    @Transactional fun decide(id: Long,decision: CategoryDecision) {
        if(decision.reason.orEmpty().length>500) throw FormValidationException(mapOf("reason" to "До 500 символів"))
        val row=dsl.fetchOne("SELECT name,status FROM category_request WHERE id=? FOR UPDATE",id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if(row.get("status",String::class.java)!="PENDING") throw ResponseStatusException(HttpStatus.CONFLICT,"Заявку вже розглянуто")
        if(decision.approved) {
            val name=row.get("name",String::class.java)!!
            if(dsl.fetchOne("SELECT 1 FROM category WHERE lower(name)=lower(?)",name)==null) dsl.execute("INSERT INTO category(name,code) VALUES(?,?)",name,"category-${UUID.randomUUID()}")
        }
        dsl.execute("UPDATE category_request SET status=?,reason=? WHERE id=?",if(decision.approved) "APPROVED" else "REJECTED",decision.reason?.trim(),id)
    }
}
