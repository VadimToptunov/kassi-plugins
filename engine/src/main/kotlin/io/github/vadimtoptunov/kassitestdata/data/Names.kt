package io.github.vadimtoptunov.kassitestdata.data

import io.github.vadimtoptunov.kassitestdata.core.Gender
import io.github.vadimtoptunov.kassitestdata.core.NameLocale

/**
 * Small bundled name pools per locale, so a persona's name is consistent with its country
 * and gender. Purely synthetic combinations — no real individuals. Countries without a
 * dedicated pool use [NameLocale.NEUTRAL].
 */
object Names {
    private data class Pool(val male: List<String>, val female: List<String>, val last: List<String>)

    private val pools: Map<NameLocale, Pool> = mapOf(
        NameLocale.EN to Pool(
            male = listOf("James", "Oliver", "Jack", "Harry", "George", "Thomas", "William", "Noah"),
            female = listOf("Olivia", "Amelia", "Emily", "Sophie", "Grace", "Charlotte", "Isla", "Ava"),
            last = listOf("Smith", "Jones", "Taylor", "Brown", "Wilson", "Evans", "Roberts", "Walker"),
        ),
        NameLocale.DE to Pool(
            male = listOf("Lukas", "Felix", "Maximilian", "Jonas", "Paul", "Leon", "Elias", "Ben"),
            female = listOf("Mia", "Emma", "Hannah", "Lena", "Anna", "Lea", "Marie", "Sophie"),
            last = listOf("Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Wagner", "Becker", "Hoffmann"),
        ),
        NameLocale.NL to Pool(
            male = listOf("Daan", "Sem", "Lucas", "Finn", "Levi", "Bram", "Milan", "Jesse"),
            female = listOf("Emma", "Julia", "Tess", "Sophie", "Sara", "Eva", "Anna", "Nina"),
            last = listOf("De Vries", "Jansen", "Van den Berg", "Bakker", "Visser", "Smit", "Meijer", "Mulder"),
        ),
        NameLocale.FR to Pool(
            male = listOf("Louis", "Gabriel", "Hugo", "Jules", "Lucas", "Adam", "Nathan", "Léo"),
            female = listOf("Emma", "Louise", "Chloé", "Léa", "Manon", "Camille", "Sarah", "Jade"),
            last = listOf("Martin", "Bernard", "Dubois", "Durand", "Moreau", "Laurent", "Girard", "Petit"),
        ),
        NameLocale.IT to Pool(
            male = listOf("Francesco", "Alessandro", "Lorenzo", "Matteo", "Andrea", "Marco", "Luca", "Davide"),
            female = listOf("Sofia", "Giulia", "Aurora", "Alice", "Ginevra", "Emma", "Chiara", "Martina"),
            last = listOf("Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", "Romano", "Colombo", "Ricci"),
        ),
        NameLocale.ES to Pool(
            male = listOf("Hugo", "Martín", "Lucas", "Mateo", "Daniel", "Pablo", "Álvaro", "Adrián"),
            female = listOf("Lucía", "Sofía", "Martina", "María", "Paula", "Julia", "Valeria", "Carla"),
            last = listOf("García", "Fernández", "González", "Rodríguez", "López", "Martínez", "Sánchez", "Pérez"),
        ),
        NameLocale.PT to Pool(
            male = listOf("João", "Rodrigo", "Martim", "Afonso", "Tomás", "Duarte", "Miguel", "Gonçalo"),
            female = listOf("Maria", "Leonor", "Matilde", "Beatriz", "Carolina", "Ana", "Mariana", "Inês"),
            last = listOf("Silva", "Santos", "Ferreira", "Pereira", "Oliveira", "Costa", "Rodrigues", "Sousa"),
        ),
        NameLocale.NEUTRAL to Pool(
            male = listOf("Alex", "Max", "Ivan", "Nikola", "Adam", "Erik", "Filip", "Luka"),
            female = listOf("Anna", "Maria", "Elena", "Sara", "Nina", "Eva", "Lena", "Sofia"),
            last = listOf("Novak", "Horvat", "Kowalski", "Popescu", "Nilsson", "Virtanen", "Petrov", "Kovac"),
        ),
    )

    fun firstName(locale: NameLocale, gender: Gender): List<String> {
        val pool = pools[locale] ?: pools.getValue(NameLocale.NEUTRAL)
        return if (gender == Gender.MALE) pool.male else pool.female
    }

    fun lastName(locale: NameLocale): List<String> =
        (pools[locale] ?: pools.getValue(NameLocale.NEUTRAL)).last
}
