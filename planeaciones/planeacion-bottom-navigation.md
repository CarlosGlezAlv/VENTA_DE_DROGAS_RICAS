# Planeación: Barra de Navegación Inferior (Bottom Navigation Bar)

**Fecha:** 2026-04-24  
**Proyecto:** Lumina Retail POS  
**Objetivo:** Reemplazar el sistema de navegación actual (botones dispersos por pantalla) con una `BottomNavigationView` de Material 3 que permita moverse libremente entre todas las secciones principales de la app.

---

## 1. Diagnóstico del Estado Actual

### Pantallas existentes

| Clase Java          | Layout XML               | Descripción                          |
|---------------------|--------------------------|--------------------------------------|
| `MainActivity`      | `activity_main.xml`      | Pantalla de bienvenida con botón "ENTRAR AL SISTEMA" |
| `Producto`          | `activity_producto.xml`  | Gestión de productos (CRUD inventario físico) |
| `Inventario`        | `activity_inventario.xml`| Punto de venta / carrito / cobro     |
| `ConfigActivity`    | `activity_config.xml`    | Configuración de la app              |

### Pantallas faltantes (por crear)

| Sección      | Clase a crear        | Layout a crear             |
|--------------|----------------------|----------------------------|
| **Historial**| `HistorialActivity`  | `activity_historial.xml`   |

### Problemas de la navegación actual
- `MainActivity` es una **splash/welcome screen**, no encaja en el modelo de barra inferior.
- Cada actividad tiene su propio botón "Volver" (`btnVolverXxx`) que hace `finish()`.
- `ConfigActivity` reinicia todo el stack (`FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK`) al guardar.
- No hay ningún estado compartido de "pestaña activa".

---

## 2. Arquitectura Objetivo

### Patrón elegido: `MainActivity` como Host + Fragments

En lugar de tener múltiples `Activity` independientes, convertiremos la app a un modelo de **Activity única con múltiples Fragments**. Este es el patrón oficial de Android para `BottomNavigationView`.

```
MainActivity (Host)
│
├── BottomNavigationView  ← barra inferior siempre visible
│
└── FragmentContainerView (NavHostFragment)
    ├── InicioFragment        (tab: Inicio)
    ├── InventarioFragment    (tab: Inventario)  ← lógica de Producto.java
    ├── VentasFragment        (tab: Ventas)      ← lógica de Inventario.java (POS)
    ├── HistorialFragment     (tab: Historial)   ← nueva pantalla
    └── ConfigFragment        (tab: Configuración) ← lógica de ConfigActivity.java
```

> **Nota:** Los archivos `Producto.java`, `Inventario.java` y `ConfigActivity.java` **no se eliminan de golpe**. Se migran gradualmente, primero construyendo los Fragments y luego deprecando las Activities antiguas.

---

## 3. Dependencias a Agregar

En `app/build.gradle.kts` agregar (si no están ya):

```kotlin
dependencies {
    // Navigation Component (Fragments + NavController)
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // Material 3 (BottomNavigationView ya incluida si se usa material 3)
    implementation("com.google.android.material:material:1.12.0")
}
```

---

## 4. Recursos XML a Crear

### 4.1 Menú de Navegación

**Archivo:** `res/menu/bottom_nav_menu.xml`

```xml
<menu>
  <item android:id="@+id/nav_inicio"        android:icon="@drawable/ic_nav_home"    android:title="Inicio" />
  <item android:id="@+id/nav_inventario"    android:icon="@drawable/ic_nav_boxes"   android:title="Inventario" />
  <item android:id="@+id/nav_ventas"        android:icon="@drawable/ic_nav_cart"    android:title="Ventas" />
  <item android:id="@+id/nav_historial"     android:icon="@drawable/ic_nav_history" android:title="Historial" />
  <item android:id="@+id/nav_configuracion" android:icon="@drawable/ic_nav_config"  android:title="Config" />
</menu>
```

### 4.2 Grafo de Navegación

**Archivo:** `res/navigation/nav_graph.xml`

Define los 5 Fragments como destinos. `InicioFragment` es el destino inicial (`app:startDestination`).

### 4.3 Iconos de la barra

Crear 5 Vector Drawables en `res/drawable/`:

| Archivo                  | Ícono Material sugerido       |
|--------------------------|-------------------------------|
| `ic_nav_home.xml`        | `home` / `storefront`         |
| `ic_nav_boxes.xml`       | `inventory_2` / `package_2`   |
| `ic_nav_cart.xml`        | `shopping_cart`               |
| `ic_nav_history.xml`     | `history` / `receipt_long`    |
| `ic_nav_config.xml`      | `settings`                    |

---

## 5. Layout Principal a Modificar

**Archivo:** `res/layout/activity_main.xml`  
Se reemplaza completamente para alojar la barra inferior y el contenedor de fragments:

```xml
<androidx.constraintlayout.widget.ConstraintLayout>

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        app:navGraph="@navigation/nav_graph"
        app:defaultNavHost="true"
        ... />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        app:menu="@menu/bottom_nav_menu"
        ... />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 6. Fragments a Crear

### 6.1 `InicioFragment`
- Muestra el nombre del negocio, logo, y un resumen rápido (total de productos en stock, último registro de venta).
- No necesita migrar ninguna Activity existente, es nueva lógica de bienvenida simplificada.

### 6.2 `InventarioFragment`
- Migración de `Producto.java` + `activity_producto.xml`.
- CRUD de productos: agregar, editar, eliminar, listar.
- El botón "Volver" desaparece; la barra inferior toma ese rol.

### 6.3 `VentasFragment`
- Migración de `Inventario.java` + `activity_inventario.xml`.
- POS: búsqueda por código, carrito, cobro, alertas de stock.
- El botón "Volver" desaparece.

### 6.4 `HistorialFragment` *(nueva)*
- Lista de ventas registradas en `registro_ventas.json` via `VentasManager`.
- Muestra: fecha, productos, total en moneda base y visual.
- Filtros opcionales por fecha (mejora futura).

### 6.5 `ConfigFragment`
- Migración de `ConfigActivity.java` + `activity_config.xml`.
- Al guardar, ya **no** se reinicia el stack completo. Solo se aplica la configuración y se navega a `InicioFragment`.

---

## 7. Cambios en `MainActivity.java`

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Aplicar config antes de inflar
        ConfigManager.getInstance(this).aplicarConfiguracionBase(this);
        setContentView(R.layout.activity_main);

        // Conectar NavController con BottomNavigationView
        NavController navController = Navigation.findNavController(
            this, R.id.nav_host_fragment
        );
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);
    }
}
```

---

## 8. Manejo del Idioma y Traducciones

Los Fragments deben llamar a `TraductorManager.getInstance(requireContext())` en `onViewCreated()` en lugar de en el constructor o `onCreate()` de la Activity. El patrón de traducción no cambia.

---

## 9. Flujo de Migración (Orden de Ejecución)

```
Paso 1 → Agregar dependencias de Navigation Component
Paso 2 → Crear iconos vectoriales de la barra (5 drawables)
Paso 3 → Crear bottom_nav_menu.xml y nav_graph.xml
Paso 4 → Reescribir activity_main.xml (host layout)
Paso 5 → Crear InicioFragment + fragment_inicio.xml
Paso 6 → Crear InventarioFragment migrando Producto.java
Paso 7 → Crear VentasFragment migrando Inventario.java
Paso 8 → Crear HistorialFragment + fragment_historial.xml (nueva)
Paso 9 → Crear ConfigFragment migrando ConfigActivity.java
Paso 10 → Actualizar MainActivity.java con NavController
Paso 11 → Deprecar/eliminar Activities antiguas y limpiar AndroidManifest.xml
Paso 12 → Pruebas: navegación, traducciones, config, alertas de stock
```

---

## 10. Consideraciones Especiales

### `ConfigActivity` y el reinicio del stack
Actualmente usa `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`. Con la arquitectura de Fragment + ViewModel, ese reinicio no será necesario: el tema se puede aplicar via `recreate()` en la `MainActivity` host o usando `AppCompatDelegate.setDefaultNightMode()` directamente.

### `AndroidManifest.xml`
Al finalizar la migración, solo `MainActivity` debe quedar declarada como `<activity>`. Las demás Activities (`Producto`, `Inventario`, `ConfigActivity`) pueden eliminarse del manifest cuando sus Fragments estén completos.

### Retrocompatibilidad
Durante la migración (pasos 1-10), las Activities antiguas seguirán funcionando en paralelo. No hay riesgo de romper funcionalidad en curso.

### Barra de navegación y teclado virtual
Configurar en el Manifest:
```xml
android:windowSoftInputMode="adjustResize"
```
Para que la barra inferior no tape el teclado en `VentasFragment` al escribir códigos de producto.

---

## 11. Resultado Esperado

- ✅ Una sola Activity host (`MainActivity`) con `BottomNavigationView` visible en todas las pantallas.
- ✅ 5 pestañas: **Inicio, Inventario, Ventas, Historial, Configuración**.
- ✅ Sin botones "Volver" manuales entre secciones principales.
- ✅ Historial de ventas accesible directamente desde la barra.
- ✅ La configuración aplica cambios sin reiniciar el stack completo.
- ✅ Compatible con el sistema de traducciones y temas existente.
