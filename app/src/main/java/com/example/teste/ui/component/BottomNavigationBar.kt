package com.example.teste.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra de Navegação Inferior Fixa com as 4 Abas:
 * 1. Agendamentos
 * 2. Cargas
 * 3. Notificações
 * 4. Perfil
 */
@Composable
fun AppBottomNavigationBar(
    selectedTab: Int,
    unreadNotificationsCount: Int = 0,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        Triple("Agendamentos", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        Triple("Cargas", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
        Triple("Notificações", Icons.Filled.Notifications, Icons.Outlined.Notifications),
        Triple("Perfil", Icons.Filled.Person, Icons.Outlined.Person)
    )

    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                val blueColor = Color(0xFF0052CC)
                val grayColor = Color(0xFF6B7280)

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    icon = {
                        if (index == 2 && unreadNotificationsCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadNotificationsCount > 99) "99+" else unreadNotificationsCount.toString(),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.second else item.third,
                                    contentDescription = item.first,
                                    tint = if (isSelected) blueColor else grayColor
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (isSelected) item.second else item.third,
                                contentDescription = item.first,
                                tint = if (isSelected) blueColor else grayColor
                            )
                        }
                    },
                    label = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.first,
                                color = if (isSelected) blueColor else grayColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(2.5.dp)
                                        .background(blueColor, shape = RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
