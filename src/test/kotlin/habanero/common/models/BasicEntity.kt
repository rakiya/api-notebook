package habanero.common.models

interface BasicEntity<T> {

    fun toRecord(): T
}