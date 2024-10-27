// ktlint-disable filename
@file:Suppress("Filename")

package edu.illinois.cs.cs124.ay2022.mp.models

/*
 * Models storing information about places retrieved from the backend server.
 *
 * You will need to understand some of the code in this file and make changes starting with MP1.
 */

class Place(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String
)

//has
fun List<Place>.search(searchString: String): List<Place> {
    var toReturn: List<Place> = mutableListOf()
    // var tosplit: ArrayList<String> = arrayListOf(".","!","?",":",";","/")
    if (this.isEmpty() || searchString.isBlank()) {
        return this
    }
    val newstring = searchString.trim().lowercase()
    for (i in this) {
        var des: String = i.description.replace(".", " ").replace("!"," ").replace("?"," ")
        des = des.replace(","," ").replace(":"," ").replace(";"," ").replace("/"," ").lowercase()

        var string2 = ""
        for (letter in des) {
            if (letter.isLetterOrDigit() || letter == ' ') {
                string2 += letter
            }
        }
        val wordss = string2.split(" ")
        if (wordss.contains(newstring)) {
            toReturn += i
        }
    }
    return toReturn
}

