package habanero.app.requests.notebook

import habanero.app.requests.ValidatedRequest
import org.hibernate.validator.constraints.Length
import javax.validation.constraints.NotEmpty

data class CreateRequest(
        @field:[
        NotEmpty(message = "必須項目です。")
        Length(max = 256, message = "256文字以下で入力してください。")
        ]
        val title: String
) : ValidatedRequest