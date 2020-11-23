package habanero.app.requests.tag

import habanero.app.requests.ValidatedRequest
import javax.validation.constraints.NotEmpty

data class TagRequest(
        @field:[NotEmpty(message = "必須項目です。")]
        val name: String
) : ValidatedRequest
