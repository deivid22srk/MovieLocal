package com.movielocal.client.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movielocal.client.data.models.Movie
import com.movielocal.client.data.models.Series
import com.movielocal.client.ui.theme.iNoxBlue
import com.movielocal.client.ui.viewmodel.UiState

// ===== HOME SCREEN =====
@Composable
fun HomeScreen(
    uiState: UiState,
    onRefresh: () -> Unit,
    onPlayMovie: (String, String, String) -> Unit,
    onOpenSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            TopBar(onOpenSettings = onOpenSettings)
        }
        
        item {
            CategoryTabs()
        }
        
        when (uiState) {
            is UiState.Initial -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iNoxBlue)
                    }
                }
            }
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iNoxBlue)
                    }
                }
            }
            is UiState.Success -> {
                if (uiState.movies.isNotEmpty() || uiState.series.isNotEmpty()) {
                    val featuredContent = uiState.movies.firstOrNull() ?: uiState.series.firstOrNull()
                    
                    if (featuredContent != null) {
                        item {
                            HeroBanner(
                                content = featuredContent,
                                onPlay = {
                                    when (featuredContent) {
                                        is Movie -> onPlayMovie(featuredContent.videoUrl, featuredContent.id, featuredContent.title)
                                        is Series -> {}
                                    }
                                }
                            )
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(24.dp))
                        ForYouSection()
                    }
                    
                    if (uiState.movies.isNotEmpty()) {
                        item {
                            ContentRow(
                                title = "Movies",
                                items = uiState.movies,
                                onItemClick = { movie ->
                                    onPlayMovie(movie.videoUrl, movie.id, movie.title)
                                }
                            )
                        }
                    }
                    
                    if (uiState.series.isNotEmpty()) {
                        item {
                            SeriesRow(
                                title = "Series",
                                items = uiState.series
                            )
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(32.dp))
                    }
                } else {
                    item {
                        EmptyState(onRefresh = onRefresh)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    ErrorState(
                        message = uiState.message,
                        onRetry = onRefresh
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MovieLocal",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile",
                tint = iNoxBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CategoryTabs() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Movies", "Series")
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tabs.size) { index ->
            Surface(
                modifier = Modifier.clickable { selectedTab = index },
                shape = RoundedCornerShape(20.dp),
                color = if (selectedTab == index) iNoxBlue else MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = tabs[index],
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun HeroBanner(
    content: Any,
    onPlay: () -> Unit
) {
    val title = when (content) {
        is Movie -> content.title
        is Series -> content.title
        else -> "Unknown"
    }
    
    val thumbnailUrl = when (content) {
        is Movie -> content.thumbnailUrl
        is Series -> content.thumbnailUrl
        else -> ""
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = title,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 200f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = iNoxBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Watch Now",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "NOW STREAMING",
                fontSize = 12.sp,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            Text(
                text = "Exclusively on MovieLocal",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ForYouSection() {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Text(
            text = "For You",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listOf("Most Watched", "Recently Add", "Best Rated")) { filter ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (filter == "Most Watched") iNoxBlue else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = filter,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (filter == "Most Watched") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ContentRow(
    title: String,
    items: List<Movie>,
    onItemClick: (Movie) -> Unit
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onItemClick(movie) }
                )
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        AsyncImage(
            model = movie.thumbnailUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun SeriesRow(
    title: String,
    items: List<Series>
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { series ->
                SeriesCard(series = series)
            }
        }
    }
}

@Composable
fun SeriesCard(series: Series) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        AsyncImage(
            model = series.thumbnailUrl,
            contentDescription = series.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun EmptyState(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Text(
                "No content available",
                fontSize = 18.sp,
                color = Color.Gray
            )
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = iNoxBlue
                )
            ) {
                Text("Refresh")
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Red
            )
            Text(
                message,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = iNoxBlue
                )
            ) {
                Text("Retry")
            }
        }
    }
}

// ===== SEARCH SCREEN =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: UiState,
    onSearch: (String) -> Unit,
    onPlayMovie: (String, String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Most Watched") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Search",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearch(it)
            },
            placeholder = { Text("Find movies, shows, and more", color = Color.Gray) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        
        Spacer(Modifier.height(24.dp))
        
        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = "For You",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listOf("Most Watched", "Recently Add", "Best Rated")) { filter ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (filter == selectedFilter) iNoxBlue else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (filter == selectedFilter) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        when (uiState) {
            is UiState.Initial -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = iNoxBlue)
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.movies.isNotEmpty()) {
                        item {
                            SearchResultsGrid(
                                title = "Movies",
                                movies = uiState.movies,
                                onMovieClick = { movie ->
                                    onPlayMovie(movie.videoUrl, movie.id, movie.title)
                                }
                            )
                        }
                    }
                    
                    if (uiState.series.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "Series",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        items(uiState.series) { series ->
                            SeriesSearchCard(series = series)
                        }
                    }
                }
            }
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = iNoxBlue)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = uiState.message,
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultsGrid(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                Card(
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                        .clickable { onMovieClick(movie) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    AsyncImage(
                        model = movie.thumbnailUrl,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesSearchCard(series: Series) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = series.thumbnailUrl,
                contentDescription = series.title,
                modifier = Modifier
                    .width(90.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = series.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
                
                Spacer(Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFB800)
                    )
                    Text(
                        text = series.rating.toString(),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ===== PROFILE SCREEN =====
@Composable
fun ProfileScreen(
    serverUrl: String,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Profile",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surface)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = iNoxBlue
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "MovieLocal User",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(Modifier.height(32.dp))
        
        ProfileOption(
            icon = Icons.Default.Settings,
            title = "Server Settings",
            subtitle = serverUrl.ifEmpty { "Not connected" },
            onClick = onOpenSettings
        )
        
        ProfileOption(
            icon = Icons.Default.Info,
            title = "About",
            subtitle = "MovieLocal v1.0",
            onClick = {}
        )
        
        ProfileOption(
            icon = Icons.Default.Download,
            title = "Downloads",
            subtitle = "Manage your downloads",
            onClick = {}
        )
        
        ProfileOption(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = "Manage notifications",
            onClick = {}
        )
    }
}

@Composable
fun ProfileOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iNoxBlue,
                modifier = Modifier.size(28.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

// ===== BOTTOM NAVIGATION =====
@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentScreen == "home") Icons.Filled.Home else Icons.Filled.Home,
                    "Home",
                    modifier = Modifier.size(26.dp)
                )
            },
            label = { Text("Home", fontSize = 12.sp) },
            selected = currentScreen == "home",
            onClick = { onNavigate("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = iNoxBlue,
                selectedTextColor = iNoxBlue,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentScreen == "search") Icons.Filled.Search else Icons.Filled.Search,
                    "Search",
                    modifier = Modifier.size(26.dp)
                )
            },
            label = { Text("Search", fontSize = 12.sp) },
            selected = currentScreen == "search",
            onClick = { onNavigate("search") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = iNoxBlue,
                selectedTextColor = iNoxBlue,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentScreen == "profile") Icons.Filled.Person else Icons.Filled.Person,
                    "Profile",
                    modifier = Modifier.size(26.dp)
                )
            },
            label = { Text("Profile", fontSize = 12.sp) },
            selected = currentScreen == "profile",
            onClick = { onNavigate("profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = iNoxBlue,
                selectedTextColor = iNoxBlue,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}

// ===== DIALOGS =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFoundDialog(
    serverIp: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.WifiTethering,
                    contentDescription = null,
                    tint = iNoxBlue
                )
                Text("Server Found!")
            }
        },
        text = {
            Text("Found MovieLocal server at:\n$serverIp\n\nWould you like to connect?")
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = iNoxBlue)
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Decline", color = Color.Gray)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDialog(
    currentServerUrl: String,
    isDiscovering: Boolean,
    onSetServerUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    onDiscover: () -> Unit
) {
    var serverUrl by remember { mutableStateOf(currentServerUrl) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = iNoxBlue
                )
                Text("Server Settings")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Enter server IP address and port:")
                
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("192.168.1.100:8080") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = iNoxBlue,
                        focusedLabelColor = iNoxBlue
                    )
                )
                
                OutlinedButton(
                    onClick = onDiscover,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDiscovering,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(iNoxBlue, iNoxBlue))
                    )
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = iNoxBlue
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Searching...", color = iNoxBlue)
                    } else {
                        Icon(Icons.Default.Search, null, tint = iNoxBlue)
                        Spacer(Modifier.width(8.dp))
                        Text("Auto Discover", color = iNoxBlue)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSetServerUrl(serverUrl) },
                enabled = serverUrl.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = iNoxBlue)
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            if (currentServerUrl.isNotEmpty()) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
