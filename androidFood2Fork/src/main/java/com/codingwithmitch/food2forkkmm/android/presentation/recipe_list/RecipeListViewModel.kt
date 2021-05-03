package com.codingwithmitch.food2forkkmm.android.presentation.recipe_list

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithmitch.food2forkkmm.domain.model.GenericMessageInfo
import com.codingwithmitch.food2forkkmm.interactors.recipe_list.SearchRecipes
import com.codingwithmitch.food2forkkmm.presentation.recipe_list.FoodCategory
import com.codingwithmitch.food2forkkmm.presentation.recipe_list.RecipeListEvents
import com.codingwithmitch.food2forkkmm.presentation.recipe_list.RecipeListState
import com.codingwithmitch.food2forkkmm.util.BuildConfig
import com.codingwithmitch.food2forkkmm.util.Logger
import com.codingwithmitch.shared.domain.util.MessageType
import com.codingwithmitch.shared.domain.util.UIComponentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class RecipeListViewModel
@Inject
constructor(
    private val searchRecipes: SearchRecipes,
): ViewModel() {

    val state: MutableState<RecipeListState> = mutableStateOf(RecipeListState())

    init {
        viewModelScope.launch {
            loadRecipes()
        }
    }

    fun onTriggerEvent(event: RecipeListEvents){
        viewModelScope.launch {
            when (event){
                RecipeListEvents.LoadRecipes -> {
                    loadRecipes()
                }
                RecipeListEvents.NewSearch -> {
                    newSearch()
                }
                RecipeListEvents.NextPage -> {
                    nextPage()
                }
                is RecipeListEvents.OnSelectCategory -> {
                    onSelectCategory(event.category)
                }
                is RecipeListEvents.OnUpdateQuery -> {
                    state.value = state.value.copy(query =  event.query)
                }
                else -> {
                    val queue = state.value.queue
                    queue.add(
                        GenericMessageInfo.Builder()
                            .id(UUID.randomUUID().toString())
                            .title("Invalid Event")
                            .messageType(MessageType.Error)
                            .uiComponentType(UIComponentType.Dialog)
                            .description("Something went wrong.")
                            .build()
                    )
                    state.value = state.value.copy(queue = queue)
                }
            }
        }
    }

    /**
     *  Called when a new FoodCategory chip is selected
     */
    private suspend fun onSelectCategory(category: FoodCategory){
        state.value = state.value.copy(selectedCategory = category)
        state.value = state.value.copy(query =  category.value)
        loadRecipes()
    }

    /**
     * Get the next page of recipes
     */
    private suspend fun nextPage(){
        state.value = state.value.copy(page = state.value.page + 1)
        loadRecipes()
    }

    /**
     * Perform a new search:
     * 1. page = 1
     * 2. list position needs to be reset
     */
    private suspend fun newSearch(){
        state.value = state.value.copy(page = 1, recipes = listOf())
        loadRecipes()
    }

    private suspend fun loadRecipes(){
        searchRecipes.execute(page = state.value.page, query = state.value.query).onEach { dataState ->
            state.value = state.value.copy(isLoading = dataState.isLoading)

            dataState.data?.let { recipeListState ->
                state.value = state.value.copy(recipes = recipeListState.recipes)
            }

            dataState.message?.let { message ->
                appendToMessageQueue(message)
            }
        }.launchIn(viewModelScope)
    }

    private fun appendToMessageQueue(messageInfo: GenericMessageInfo){
        val queue = state.value.queue
        queue.add(messageInfo)
        state.value = state.value.copy(queue = queue)
    }

}





