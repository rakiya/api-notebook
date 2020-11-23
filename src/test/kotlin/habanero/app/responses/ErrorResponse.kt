package habanero.app.responses

import io.jooby.StatusCode

data class ErrorResponse(val errors: MutableList<Error> = mutableListOf(), val statusCode: StatusCode? = null) {

    data class Error(val field: String, val descriptions: MutableList<String> = mutableListOf())

    fun add(field: String, messages: List<String>): ErrorResponse {
        if (errors.find { it.field == field } == null)
            errors.add(ErrorResponse.Error(field))

        errors.find { it.field == field }!!.descriptions.addAll(messages)

        return this
    }

}