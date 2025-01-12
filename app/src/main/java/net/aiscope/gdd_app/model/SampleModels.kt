package net.aiscope.gdd_app.model

import net.aiscope.gdd_app.extensions.replaceElementAt
import java.io.File
import java.util.Calendar

data class Sample(
    val id: String,
    val healthFacility: String,
    val microscopist: String,
    val disease: String,
    val preparation: SamplePreparation? = null,
    val microscopeQuality: MicroscopeQuality? = null,
    val captures: Captures = Captures(),
    val metadata: SampleMetadata = SampleMetadata(),
    val status: SampleStatus = SampleStatus.Incomplete,
    val createdOn: Calendar = Calendar.getInstance(),
    val lastModified: Calendar = Calendar.getInstance()
) {
    fun addNewlyCapturedImage(path: File) = copy(captures = captures.newCapture(path))

    fun upsertMask(path: File, isEmpty: Boolean) = copy(captures = captures.upsertMask(path, isEmpty))

    fun nextImageName(): String = "${id}_image_${captures.completedCaptureCount()}"

    fun nextMaskName(): String = "${id}_mask_${captures.completedCaptureCount()}"
}

enum class SampleStatus(val id: Short) {
    Incomplete(0),
    ReadyToUpload(1),
    Uploaded(2)
}

data class SamplePreparation(
    val waterType: WaterType,
    val usesGiemsa: Boolean,
    val giemsaFP: Boolean,
    val usesPbs: Boolean,
    val usesAlcohol: Boolean,
    val reusesSlides: Boolean
)

@Suppress("MagicNumber") // these are IDs
enum class WaterType(val id: Int) {
    DISTILLED(1),
    BOTTLED(2),
    TAP(3),
    WELL(4)
}

data class MicroscopeQuality(
    val isDamaged: Boolean,
    val magnification: Int
)

data class Captures(
    val inProgressCapture: InProgressCapture?,
    val completedCaptures: List<CompletedCapture>
) {

    constructor() : this(null, emptyList())

    fun newCapture(path: File): Captures = Captures(InProgressCapture(path), completedCaptures)

    fun upsertMask(maskPath: File, isEmpty: Boolean): Captures {
        val indexOfExistingCaptureForMask = completedCaptures.indexOfFirst { it.mask == maskPath }

        return when {
            indexOfExistingCaptureForMask >= 0 && inProgressCapture != null ->
                // we somehow broke the business logic
                throw IllegalStateException(
                    "Trying to add mask $maskPath at index $indexOfExistingCaptureForMask " +
                            "when inProgressCapture is not null ($inProgressCapture)"
                )
            inProgressCapture != null -> // indexOfCaptureIfMaskExists < 0
                Captures(
                    null,
                    completedCaptures + CompletedCapture(
                        inProgressCapture.image, maskPath, isEmpty
                    )
                )
            else -> // inProgressCapture == null && indexOfCaptureIfMaskExists < 0
                Captures(
                    null,
                    completedCaptures.replaceElementAt(
                        indexOfExistingCaptureForMask,
                        completedCaptures[indexOfExistingCaptureForMask].copy(maskIsEmpty = isEmpty)
                    )
                )
        }
    }

    fun completedCaptureCount(): Int {
        return completedCaptures.size
    }

}

sealed class Capture
data class InProgressCapture(val image: File) : Capture()
data class CompletedCapture(val image: File, val mask: File, val maskIsEmpty: Boolean) : Capture()

data class SampleMetadata(
    val smearType: SmearType = SmearType.THIN,
    val species: MalariaSpecies = MalariaSpecies.P_FALCIPARUM,
    val comments: String = ""
)

@Suppress("MagicNumber") // these are IDs
enum class MalariaSpecies(val id: Int) {
    P_FALCIPARUM(1),
    P_VIVAX(2),
    P_OVALE(3),
    P_MALARIAE(4),
    P_KNOWLESI(5)
}

enum class SmearType(val id: Int) {
    THIN(1),
    THICK(2)
}
