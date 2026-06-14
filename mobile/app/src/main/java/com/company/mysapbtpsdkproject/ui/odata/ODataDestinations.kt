package com.company.mysapbtpsdkproject.ui.odata

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.sap.cloud.mobile.kotlin.odata.EntityType
import com.sap.cloud.mobile.kotlin.odata.NavigationProperty
import com.sap.cloud.mobile.kotlin.odata.Property
import com.sap.cloud.mobile.kotlin.odata.StructureType

const val NAV_ENTITY_LIST = "_entities_list"
const val NAV_ENTITY_NAV_LIST = "_navigate"


interface ODataNavigationCommand {
    val route: String
    val arguments: List<NamedNavArgument>
}

const val navigationPropertyNameArg = "navigation_property_name"

object EntitySetsDest : ODataNavigationCommand {
    override val arguments: List<NamedNavArgument>
        get() = listOf()
    override val route: String = "entity_sets"
}


class EntityNavigationCommands(private val entityType: StructureType) {

    val entityListNav = object : ODataNavigationCommand {
        override val arguments: List<NamedNavArgument>
            get() = listOf()
        override val route: String = "${entityType.localName}/$NAV_ENTITY_LIST"
    }

    val toEntitiesNav = object : ODataNavigationCommand {
        override val arguments: List<NamedNavArgument>
            get() = listOf(
                navArgument(navigationPropertyNameArg) { type = NavType.StringType },
            )
        override val route: String = "${entityType.localName}/$NAV_ENTITY_NAV_LIST/{$navigationPropertyNameArg}/$NAV_ENTITY_LIST"
    }
}

fun NavHostController.navigateToEntityList(entityType: EntityType) {
    this.navigate("${entityType.localName}/$NAV_ENTITY_LIST")
}

// support navigation to navigation or complex property list
fun NavHostController.navigateToNavigatePropertyList(
    navProp: Property
) {
    val targetTypeName = if (navProp is NavigationProperty) navProp.relatedEntityType.localName else
        if (navProp.isComplex) navProp.complexType.localName else throw IllegalArgumentException("Invalid property type")
    this.navigate(
        "${targetTypeName}/$NAV_ENTITY_NAV_LIST/${navProp.name}/$NAV_ENTITY_LIST"
    )
}
