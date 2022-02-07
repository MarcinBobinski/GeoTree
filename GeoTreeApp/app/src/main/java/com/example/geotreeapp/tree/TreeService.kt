package com.example.geotreeapp.tree

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LiveData
import com.example.geotreeapp.base.retry
import com.example.geotreeapp.tree.api_um_waw.UmWawApi
import com.example.geotreeapp.tree.tree_db.infrastructure.Tree
import com.example.geotreeapp.tree.tree_db.infrastructure.TreeDatabase
import com.example.geotreeapp.tree.tree_db.TreeRepository
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception
import java.lang.NullPointerException

class TreeService(): Service() {
    inner class TreeServiceBinder: Binder() { fun getService(): TreeService = this@TreeService }
    private val binder = TreeServiceBinder()
    override fun onBind(intent: Intent?): IBinder { return binder }

    companion object{
        private const val PAGE_SIZE = 25
    }

    private lateinit var treeRepository: TreeRepository

    var allTrees: LiveData<List<Tree>>? = null
    var treesNumber: LiveData<Long>? = null
    private var isUpdatingData = false

    val job = SupervisorJob()
    val treeServiceScope = CoroutineScope(CoroutineName("TreeServiceScope") + Dispatchers.IO + job)

    override fun onCreate() {
        super.onCreate()
        val treeDao = TreeDatabase.getInstance(application).treeDao()
        treeRepository = TreeRepository(treeDao)
        allTrees = treeRepository.trees
        treesNumber = treeRepository.size
    }

    fun updateData() {
        if(!isUpdatingData){
            isUpdatingData = true
            treeServiceScope.launch {
                Timber.i("Starting updating trees data")
                val treesNum = fetchTreesNumber()
                val arguments = (1..treesNum).toList().chunked(PAGE_SIZE)
                val trees: List<Tree> = fetchTrees(arguments)
                Timber.i("Clearing tree database")
                treeRepository.deleteAll()
                Timber.i("Filling tree database")
                treeRepository.addTrees(trees)
                Timber.i("Finished updating trees data")
                isUpdatingData = false
            }
        }
    }

    suspend fun fetchTrees(treesIds: List<List<Long>>): List<Tree>{
        return withContext(Dispatchers.IO){
            treesIds.map {
                ensureActive()
                retry {
                    val response = UmWawApi.fetchResponseWithTreeId(it)
                    if(response.isSuccessful){
                        response.body()?.getTrees() ?: throw NullPointerException("TreeServiceViewModel.fetchTrees response body is null for treesIds: [${it.joinToString(separator = ", ")}]")
                    } else {
                        throw Exception("TreeServiceViewModel.fetchTrees unknown error, response status code: ${response.code()}, ${response.errorBody().toString()}")
                    }
                }
            }
                .flatten()
                .map { Tree(it.id, it.x, it.y) }
        }
    }

    suspend fun fetchTreesNumber(): Long{
        return withContext(Dispatchers.IO){
            val response = UmWawApi.fetchResponseWithNoTrees()
            if(response.isSuccessful) {
                response.body()?.getSize() ?: throw NullPointerException("TreeServiceViewModel.fetchTreesNumber response body is null")
            } else {
                throw NullPointerException("TreeServiceViewModel.fetchTreesNumber unknown error, response status code: ${response.code()}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}