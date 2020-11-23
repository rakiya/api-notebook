package habanero.exceptions

class NotFoundException(val type: Any, val item: Any) : RuntimeException()
