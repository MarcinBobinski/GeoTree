package com.example.treedb.Controller

import com.example.treedb.tree.Tree
import com.example.treedb.tree.TreeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
internal class TreeController(
    private val treeService: TreeService
) {

    @GetMapping("/trees")
    fun fetchTrees(
        @RequestParam xMin: Double,
        @RequestParam yMin: Double,
        @RequestParam xMax: Double,
        @RequestParam yMax: Double
    ) : ResponseEntity<List<Tree>>{
        //TODO: add option when data is not loaded yet
        return ResponseEntity.ok(treeService.fetchTreesInArea(xMin, yMin, xMax, yMax))
    }


}