package com.example.treedb.tree.UMWarszawaApi

import com.example.treedb.tree.Tree

internal interface UMWarszawaApiClient {
    fun fetchTrees(page: Int): List<Tree>
    fun fetchPageCount(): Int
}
