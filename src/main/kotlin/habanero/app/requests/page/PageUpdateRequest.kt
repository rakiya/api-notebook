package habanero.app.requests.page

import habanero.app.requests.ValidatedRequest
import org.hibernate.validator.constraints.Length
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

data class PageUpdateRequest(

        @field:[NotNull(message = "必須項目です。")]
        val notebookId: Long,

        @field:[NotEmpty(message = "必須項目です。") Length(max = 256, message = "256文字以下で入力してください。")]
        val title: String,

        @field:[NotNull(message = "必須項目です。")]
        val order: Int?

) : ValidatedRequest
