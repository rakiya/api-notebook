package habanero.app.requests.section

import habanero.app.requests.ValidatedRequest
import javax.validation.constraints.NotNull

data class SectionCreateRequest(
        @field:[NotNull(message = "必須項目です。")]
        val content: String
) : ValidatedRequest
