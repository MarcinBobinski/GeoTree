package com.example.geotreeapp.tree

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.geotreeapp.common.retrySuspend
import com.example.geotreeapp.tree.api_um_waw.UmWawApi
import com.example.geotreeapp.tree.tree_db.infrastructure.Tree
import com.example.geotreeapp.tree.tree_db.infrastructure.TreeDatabase
import com.example.geotreeapp.tree.tree_db.TreeRepository
import com.example.geotreeapp.tree.tree_db.infrastructure.TreeStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.Exception
import java.lang.NullPointerException
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class TreeService(): Service() {
    inner class TreeServiceBinder: Binder() { fun getService(): TreeService = this@TreeService }
    private val binder = TreeServiceBinder()
    override fun onBind(intent: Intent?): IBinder { return binder }

    private val serviceThreadPool = Executors.newFixedThreadPool(4)

    private val _updateStatus: MutableLiveData<Int?> = MutableLiveData(null)
    val updateStatus: LiveData<Int?>
        get() = _updateStatus

    companion object{
        private const val PAGE_SIZE = 25
    }

    private lateinit var treeRepository: TreeRepository

    lateinit var allTrees: LiveData<List<Tree>>
    lateinit var treesNumber: LiveData<Long>
    private var isUpdatingData = false

    private val job = SupervisorJob()
    private val treeServiceScope = CoroutineScope(CoroutineName("TreeServiceScope") + Dispatchers.IO + job)

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
            _updateStatus.value = 0
            treeServiceScope.launch {
                Timber.i("Starting updating trees data")
                val treesNum = fetchTreesNumber()
                val trees: List<Tree> = fetchTrees(treesNum)
                Timber.i("Clearing tree database")
                treeRepository.deleteAll()
                Timber.i("Filling tree database")
                treeRepository.addTrees(trees)
                Timber.i("Finished updating trees data")
                withContext(Dispatchers.Main){
                    _updateStatus.run {
                        value = 100
                        value = null
                    }
                }
                isUpdatingData = false
            }
        }
    }

    private suspend fun fetchTrees(treesNum: Long): List<Tree> {
        return withContext(Dispatchers.IO) {
            (1..treesNum).toList().chunked(PAGE_SIZE).asFlow().map {
                ensureActive()
                retrySuspend {
                    val response = UmWawApi.fetchResponseWithTreeId(it)

                    val currentUpdateStatus = ((it.first() / treesNum) * 100).toDouble().toInt()
                    val previousUpdateStatus = (((it.first() - PAGE_SIZE) / treesNum) * 100).toInt()
                    if(currentUpdateStatus > previousUpdateStatus) {
                        withContext(Dispatchers.Main){
                            _updateStatus.value = currentUpdateStatus
                        }
                    }
                    if (response.isSuccessful) {
                        response.body()?.getTrees() ?: throw NullPointerException(
                            "TreeServiceViewModel.fetchTrees response body is null for treesIds: [${it.joinToString(separator = ", ")}]"
                        )
                    } else {
                        throw Exception(
                            "TreeServiceViewModel.fetchTrees unknown error, response status code: ${response.code()}, ${
                                response.errorBody().toString()
                            }"
                        )
                    }
                }
            }
                .flowOn(serviceThreadPool.asCoroutineDispatcher())
                .toList()
                .flatten()
                .map { Tree(it.id, it.x, it.y, it.inv_number, it.type, TreeStatus.NOT_VERIFIED) }
        }
    }

    private suspend fun fetchTreesNumber(): Long{
        return withContext(Dispatchers.IO){
            val response = UmWawApi.fetchResponseWithNoTrees()
            if(response.isSuccessful) {
                response.body()?.getSize() ?: throw NullPointerException("TreeServiceViewModel.fetchTreesNumber response body is null")
            } else {
                throw NullPointerException("TreeServiceViewModel.fetchTreesNumber unknown error, response status code: ${response.code()}")
            }
        }
    }

    fun updateTree(tree: Tree){
        treeServiceScope.launch {
            treeRepository.updateTree(tree)
        }
    }

    fun updateTrees(trees: List<Tree>){
        treeServiceScope.launch {
            treeRepository.updateTrees(trees)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

