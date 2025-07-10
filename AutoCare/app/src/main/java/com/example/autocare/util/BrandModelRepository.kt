package com.example.autocare.util
import android.content.Context
import android.util.Log
data class Brand(val id: String, val name: String)
// data class Model(val brandId: String, val name: String)

class BrandModelRepository(private val context: Context) {
    private val TAG = "BrandModelRepo"

    private val brandList: List<Brand> by lazy {
        loadBrands().also { Log.d(TAG, "Loaded ${'$'}{it.size} brands") }
    }

    // private val modelList: List<Model> by lazy {
    //     loadModels().also { Log.d(TAG, "Loaded ${'$'}{it.size} models") }
    // }

    // private val modelsByBrand: Map<String, List<Model>> by lazy {
    //     modelList.groupBy { it.brandId }
    // }

    private fun loadBrands(): List<Brand> =
        context.assets.open("ID_y_Nombre_de_Marca.csv")
            .bufferedReader()
            .lineSequence()
            .drop(1)
            .mapNotNull { line ->
                val cols = line.split(",")
                if (cols.size >= 3) Brand(id = cols[1].trim(), name = cols[2].trim()) else null
            }
            .toList()

    // private fun loadModels(): List<Model> =
    //     context.assets.open("Models_de_turismes_i_autocaravanes.csv")
    //         .bufferedReader()
    //         .lineSequence()
    //         .drop(1)
    //         .mapNotNull { line ->
    //             // CSV columns: idx,Nom,Tipus,brandId
    //             val cols = line.split(",")
    //             if (cols.size >= 4) Model(brandId = cols[3].trim(), name = cols[1].trim()) else null
    //         }
    //         .toList()

    fun getBrands(): List<Brand> = brandList

    // fun getModelsByBrandId(brandId: String): List<Model> =
    //     modelsByBrand[brandId] ?: emptyList()
}