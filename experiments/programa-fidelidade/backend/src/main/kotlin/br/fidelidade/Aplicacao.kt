package br.fidelidade

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Aplicacao

fun main(args: Array<String>) {
    runApplication<Aplicacao>(*args)
}
