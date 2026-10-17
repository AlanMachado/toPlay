package br.fidelidade.catalogo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Entity
@Table(name = "catalogo")
class ItemCatalogo(
    val nome: String,
    val descricao: String?,
    @Column(name = "custo_pontos") val custoPontos: Long,
    val ativo: Boolean = true,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)

interface CatalogoRepository : JpaRepository<ItemCatalogo, Long> {
    fun findByAtivoTrueOrderByCustoPontos(): List<ItemCatalogo>
}

data class ItemCatalogoDto(val id: Long, val nome: String, val descricao: String?, val custoPontos: Long)

@RestController
@RequestMapping("/api/catalogo")
class CatalogoController(private val catalogo: CatalogoRepository) {
    @GetMapping
    fun listar(): List<ItemCatalogoDto> =
        catalogo.findByAtivoTrueOrderByCustoPontos().map { ItemCatalogoDto(it.id!!, it.nome, it.descricao, it.custoPontos) }
}
