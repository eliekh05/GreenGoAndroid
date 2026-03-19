package com.greengo.app.data

import java.util.UUID
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Trivia Models
// ─────────────────────────────────────────────────────────────────────────────

data class TriviaQuestion(
    val id: UUID = UUID.randomUUID(),
    val question: String,
    val answers: List<TriviaAnswer>,
    val correctIndex: Int
)

data class TriviaAnswer(
    val id: UUID = UUID.randomUUID(),
    val text: String,
    val imageName: String,
    val explanation: String
)

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - All 5 trivia rounds
// ─────────────────────────────────────────────────────────────────────────────

val allTriviaQuestions: List<TriviaQuestion> = listOf(
    TriviaQuestion(
        question = "When exploring a destination, which mode of transportation is the most eco-friendly choice?",
        answers = listOf(
            TriviaAnswer(
                text = "Sustainable Public Transportation",
                imageName = "sustainablepublictransportation1",
                explanation = "Sustainable public transportation reduces negative environmental impacts by emitting fewer greenhouse gases, reducing traffic congestion, optimizing energy usage, conserving natural resources, and decreasing air and noise pollution in urban areas."
            ),
            TriviaAnswer(
                text = "Car",
                imageName = "CAR",
                explanation = "Cars increase negative environmental impacts on Earth by emitting greenhouse gases, air pollution, relying on non-renewable resources, and requiring significant land use for infrastructure."
            ),
            TriviaAnswer(
                text = "Motorcycle",
                imageName = "motorcycle_removebg_preview",
                explanation = "Motorcycles increase negative environmental impacts on Earth by emitting air and noise pollution, having lower fuel efficiency than cars, and requiring land for infrastructure, which can contribute to habitat loss and urban sprawl."
            )
        ),
        correctIndex = 0
    ),
    TriviaQuestion(
        question = "As a tourist, which of the following activities should you consider while traveling?",
        answers = listOf(
            TriviaAnswer(
                text = "Participating in Guided Nature Walks",
                imageName = "guidedtripwalks",
                explanation = "Participating in guided nature walks allows tourists to engage with the environment, learn about local conservation efforts, and support sustainable tourism practices."
            ),
            TriviaAnswer(
                text = "Eating at Local Restaurants",
                imageName = "Eatingatlocalrestaurants",
                explanation = "Eating at local restaurants allows tourists to engage with the local culture, support sustainable food practices, and contribute to the economic development of the local community."
            ),
            TriviaAnswer(
                text = "Booking All-Inclusive Package Tours",
                imageName = "bookingall_inclusivepackagetours",
                explanation = "Booking all-inclusive package tours can lead to unsustainable tourism practices that negatively affect the environment and local communities."
            )
        ),
        correctIndex = 0
    ),
    TriviaQuestion(
        question = "Which practice supports sustainable shopping while traveling?",
        answers = listOf(
            TriviaAnswer(
                text = "Buying Locally Sourced Products",
                imageName = "localbusiness",
                explanation = "Purchasing locally sourced products supports local economies, reduces carbon emissions associated with transportation, and promotes sustainable practices."
            ),
            TriviaAnswer(
                text = "Souvenirs from Endangered Species",
                imageName = "souvenirsfromendangeredspecies",
                explanation = "Buying souvenirs made from endangered species promotes the illegal wildlife trade, threatens the survival of already endangered species, and contributes to environmental degradation."
            ),
            TriviaAnswer(
                text = "Shopping at Large Chain Stores",
                imageName = "largechainstores",
                explanation = "Large chain stores often prioritize profits over sustainability and may contribute to negative environmental impacts."
            )
        ),
        correctIndex = 0
    ),
    TriviaQuestion(
        question = "When exploring a natural area, which camping option demonstrates environmentally friendly practices?",
        answers = listOf(
            TriviaAnswer(
                text = "Using a Reusable Camping Stove",
                imageName = "campstove",
                explanation = "Reusable camping stoves or fire pits reduce the consumption of single-use materials like disposable charcoal or wood-burning grills and help minimize air pollution."
            ),
            TriviaAnswer(
                text = "Leaving Campfire Unattended",
                imageName = "unnamed",
                explanation = "Leaving a campfire unattended increases the risk of wildfires and poses a threat to the surrounding ecosystem."
            ),
            TriviaAnswer(
                text = "Using Disposable Grills",
                imageName = "disposablegrill",
                explanation = "Single-use disposable grills contribute to waste, pollution, and can harm the environment when left behind."
            )
        ),
        correctIndex = 0
    ),
    TriviaQuestion(
        question = "When exploring a natural site, which activity demonstrates responsible wildlife viewing?",
        answers = listOf(
            TriviaAnswer(
                text = "Observing Animals from a Safe Distance",
                imageName = "touristswatchinganimals",
                explanation = "Maintaining a safe distance minimizes stress and disturbance to wildlife, allowing them to carry on their natural behaviors undisturbed."
            ),
            TriviaAnswer(
                text = "Using Flash Photography",
                imageName = "photographing",
                explanation = "Using flash photography can startle or blind animals, affecting their natural behavior and potentially causing harm."
            ),
            TriviaAnswer(
                text = "Feeding Wild Animals",
                imageName = "feedinganimal1",
                explanation = "Touching or feeding wild animals can disrupt their natural behavior, cause stress, and potentially harm both the animals and yourself."
            )
        ),
        correctIndex = 0
    )
)
