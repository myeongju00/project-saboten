package commonClient.di

import commonClient.data.cache.createDataStore
import commonClient.data.cache.dataStoreFileName
import commonClient.presentation.GlobalAppViewModel
import commonClient.presentation.main.HomeScreenViewModel
import commonClient.presentation.main.MoreScreenViewModel
import commonClient.presentation.main.CategoryScreenViewModel
import commonClient.presentation.main.SearchScreenViewModel
import commonClient.presentation.main.ProfileScreenViewModel
import commonClient.presentation.post.DetailPostScreenViewModel
import commonClient.presentation.post.WritePostScreenViewModel
import commonClient.presentation.login.LoginScreenViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

actual fun platformModule() = module {
    single { createDataStore { androidContext().filesDir.resolve(dataStoreFileName).absolutePath } }

    viewModelOf(::GlobalAppViewModel)
    viewModelOf(::HomeScreenViewModel)
    viewModelOf(::MoreScreenViewModel)
    viewModelOf(::CategoryScreenViewModel)
    viewModelOf(::SearchScreenViewModel)
    viewModelOf(::ProfileScreenViewModel)
    viewModelOf(::WritePostScreenViewModel)
    viewModelOf(::DetailPostScreenViewModel)
    viewModelOf(::LoginScreenViewModel)

}