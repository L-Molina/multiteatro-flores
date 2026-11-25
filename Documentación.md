# Multiteatro API — Documentación de endpoints

- **Base URL:** `http://localhost:8080`
- **Frontend en desarrollo:** CORS habilitado para `localhost` / `127.0.0.1` en los puertos `3000`, `4173`, `5173` y `5500`.

## 0. CORS

`config/CorsConfig` habilita CORS para `/api/**` con los métodos `GET`, `POST`, `PUT`, `DELETE` y `OPTIONS`, y los headers `Authorization` y `Content-Type`.

Sin esto, el navegador bloquea los `fetch` cuando el frontend corre en otro origen. Postman no aplica CORS, por eso un request puede funcionar ahí y fallar en el navegador. Si el frontend usa otro puerto, agregalo en `CorsConfig`.

## 1. Autenticación

El token se obtiene con `POST /api/auth/login` y es un UUID. Si reiniciás la aplicación, los tokens emitidos dejan de funcionar y hay que hacer login de nuevo.

No hay dos tipos de token ni expiración por tiempo. El mecanismo es:

1. `POST /api/auth/login` valida email + contraseña contra el hash BCrypt en la base.
2. Si son correctos, se genera un UUID y se guarda en un `ConcurrentHashMap` (`SesionService`) asociado al usuario.
3. `ContextoSesion` lee el header en cada request, busca el UUID en el mapa y devuelve el `Usuario`. Si no lo encuentra, lanza `NoAutenticadoException` → 401.

> **Para el frontend:** cualquier **401** significa "volvé a iniciar sesión". No asumas que una sesión sigue viva porque el servidor se puede haber reiniciado.

## 2. Roles y autorización

`Usuario.rol` es un enum con tres valores: `CLIENTE`, `ADMIN`, `SUPERADMIN`.

| Regla | Comportamiento |
|---|---|
| `requiereRol(actual, ADMIN)` | Lo pasan `ADMIN` y `SUPERADMIN`. Un `CLIENTE` recibe **403**. |
| `requiereRol(actual, SUPERADMIN)` | Solo `SUPERADMIN`. |
| `requiereAdminDeTeatro(actual, teatro)` | El `ADMIN` que es dueño de ese teatro, o `SUPERADMIN`. Un admin de otro teatro recibe **403** `"No administrás este teatro"`. |

`SUPERADMIN` siempre pasa cualquier chequeo (early-return en `AutorizacionService`).

### Acceso público (sin token)

Para que un visitante pueda ver la cartelera antes de registrarse, estos endpoints son públicos:

- `GET /api/obras` y `GET /api/obras/{id}`
- `GET /api/funciones` y `GET /api/funciones/{id}`
- `GET /api/teatros` y `GET /api/teatros/{id}`

El resto de los `GET` exige login. En particular, el mapa de butacas (`GET /api/funciones/{id}/asientos`) exige token: la compra se decide después de elegir teatro y butaca, y a partir de ahí el usuario ya tiene que estar registrado.

### Diagrama de permisos por endpoint

| Endpoint family | Sin token | CLIENTE | ADMIN | SUPERADMIN |
|---|:---:|:---:|:---:|:---:|
| `GET /api/obras`, `/api/funciones`, `/api/teatros` | ✓ | ✓ | ✓ | ✓ |
| `GET` de salas, asientos, tipos-asiento y mapa de butacas | ✗ 401 | ✓ | ✓ | ✓ |
| `POST/PUT/DELETE /api/obras`, `/api/tipos-asiento` | ✗ 401 | ✗ 403 | ✓ | ✓ |
| `POST/PUT/DELETE /api/teatros` (crear/borrar) | ✗ 401 | ✗ 403 | ✗ 403 | ✓ |
| `PUT/DELETE /api/teatros/{id}` (editar) | ✗ 401 | ✗ 403 | ✓ solo su teatro | ✓ |
| `POST/PUT/DELETE /api/salas`, `/api/asientos`, `/api/funciones` | ✗ 401 | ✗ 403 | ✓ solo su teatro | ✓ |
| `POST /api/compras` | ✗ 401 | ✓ | ✓ | ✓ |
| `GET /api/compras`, `GET /api/entradas` | ✗ 401 | solo las propias | solo las propias | todas |
| `GET/POST/DELETE /api/usuarios` | ✗ 401 | ✗ 403 | ✗ 403 | ✓ |
| `POST /api/usuarios` con `rol: CLIENTE` | ✗ 401 | ✓ | ✓ | ✓ |
| `PUT /api/usuarios/{id}` | ✗ 401 | solo el propio | solo el propio | cualquiera |
| `DELETE /api/entradas/{id}` | ✗ 401 | solo suyas | solo suyas | cualquiera |
| `GET/POST /api/usuarios` (crear ADMIN o SUPERADMIN) | ✗ 401 | ✗ 403 | ✗ 403 | ✓ |

## 3. Códigos de respuesta

| Código | Cuándo | Body |
|---|---|---|
| `200 OK` | Éxito. **Todos los writes devuelven 200**, no 201. | JSON con el recurso, o vacío en los DELETE |
| `400 Bad Request` | Body inválido para una compra: sin asientos, asientos repetidos o de otra sala | Texto plano |
| `401 Unauthorized` | Falta el header, no usa el prefijo `Bearer `, el token no existe (app reiniciada) o el login es incorrecto | Texto plano |
| `403 Forbidden` | El rol no alcanza | Texto plano: `No tenés permisos para esta acción` / `No administrás este teatro` |
| `404 Not Found` | El `id` de un recurso no existe | Texto plano: `Obra no encontrada`, `Función no encontrada`, etc. |
| `409 Conflict` | El asiento ya fue comprado para esa función | Texto plano: `Uno de los asientos elegidos ya no está disponible para esta función` |
| `500 Internal Server Error` | Falta un campo obligatorio en el body u otro error no controlado | Texto plano |

> Los errores son **texto plano**, no JSON. En el frontend hay que leer `response.text()` en los errores, no `response.json()`.
> Un recurso inexistente ahora devuelve **404**, no 500.

## 4. Convenciones de los bodies

- Los `id` son autogenerados: **nunca** mandarlos al crear.
- Las relaciones viajan solo con su `id`: `"teatro": {"id": 1}`, no el objeto completo.
- En los `PUT` los campos se escriben **siempre** (no hay update parcial), así que hay que mandar el objeto completo. Si se omite un campo `NOT NULL`, la request falla.
- `rol` es case-sensitive: `CLIENTE`, `ADMIN`, `SUPERADMIN`.
- `Obra.tipo` es case-sensitive: `TEATRAL`, `STAND_UP`, `CONCIERTO`.
- `passwordHash` ya no se serializa en las respuestas.

## 5. Endpoints

### 5.1 Auth — `/api/auth`

Sin autenticación (excepto `logout`, que es tolerante si no mandás token).

#### `POST /api/auth/login`
Devuelve el token. Es el único punto de entrada al sistema.

Request:
```json
{ "email": "superadmin@multiteatroflores.com", "password": "superadmin123" }
```
Response `200`:
```json
{
  "token": "e08f5429-8a16-47d6-b3a9-1e688c599528",
  "usuario": { "id": 1, "nombre": "Lautaro", "email": "superadmin@multiteatroflores.com", "rol": "SUPERADMIN" }
}
```
Errores: `401 Credenciales inválidas` si el email no existe o la contraseña no coincide.

#### `POST /api/auth/register`
Crea un `CLIENTE`. Ojo: la clave es **`name`**, no `nombre` (a diferencia del resto de la API).

Request:
```json
{ "name": "Nuevo", "email": "nuevo@example.com", "password": "clave123" }
```
Response `200`: el mismo `{ "token": ..., "usuario": ... }` que el login. Cualquier campo `rol` que se mande se **ignora**: siempre se crea `CLIENTE`.

#### `POST /api/auth/logout`
Sin body. Invalida el token en el mapa de sesiones. Response `200 {"mensaje":"Sesión cerrada"}`. Después de esto, usar el mismo token devuelve **401**.

### 5.2 Teatro — `/api/teatros`

#### `GET /api/teatros` → `200` `[Teatro]` — **público**
#### `GET /api/teatros/{id}` → `200` `Teatro` — **público** — `404` si no existe
#### `POST /api/teatros` — **solo SUPERADMIN**
```json
{ "nombre": "Teatro Central", "direccion": "Av. Corrientes 123", "admin": { "id": 2 } }
```
`nombre` obligatorio. `admin` es opcional (si se manda, debe ser el id de un `ADMIN`).

#### `PUT /api/teatros/{id}` — **admin dueño del teatro o SUPERADMIN**
```json
{ "nombre": "Teatro Central", "direccion": "Av. Corrientes 123" }
```
El `admin` **no se modifica** con este endpoint.

#### `DELETE /api/teatros/{id}` — **solo SUPERADMIN** → `200` body vacío

### 5.3 Sala — `/api/salas`

#### `GET /api/salas` → `200` `[Sala]`
#### `GET /api/salas/{id}` → `200` `Sala` — `404` si no existe
#### `POST /api/salas` — **admin del teatro o SUPERADMIN**
```json
{ "nombre": "Sala 1", "capacidad": 25, "teatro": { "id": 1 } }
```
`teatro.id` obligatorio.

#### `PUT /api/salas/{id}` — **admin del teatro de esa sala**
```json
{ "nombre": "Sala 1", "capacidad": 25 }
```
El `teatro` no se modifica.

#### `DELETE /api/salas/{id}` — **admin del teatro** → `200` body vacío

### 5.4 Obra — `/api/obras`

Todas las obras viven en la misma tabla y se separan por `tipo`.

#### `GET /api/obras` → `200` `[Obra]` — **público**
#### `GET /api/obras/{id}` → `200` `Obra` — **público** — `404` si no existe
#### `POST /api/obras` — **ADMIN o SUPERADMIN**
```json
{
  "titulo": "Hamlet",
  "tipo": "TEATRAL",
  "precio": 30000.0,
  "clasificacion": "+13",
  "descripcion": "Tragedia de Shakespeare.",
  "duracionMinutos": 150
}
```
`titulo`, `tipo`, `precio`, `clasificacion` y `duracionMinutos` son obligatorios. `tipo` es `TEATRAL`, `STAND_UP` o `CONCIERTO`.

Precios base de la base de ejemplo: teatral `$30.000`, stand-up `$20.000`, concierto `$12.500`.

#### `PUT /api/obras/{id}` — **ADMIN o SUPERADMIN**
Se mandan los seis campos completos.

#### `DELETE /api/obras/{id}` — **ADMIN o SUPERADMIN** → `200` body vacío

### 5.5 Tipo de asiento — `/api/tipos-asiento`

Path con guion, `tipos-asiento`.

El tipo de asiento ya **no guarda un precio absoluto**: guarda un **multiplicador** sobre el precio base de la función. Así una misma butaca sirve para obras de distinto precio.

#### `GET /api/tipos-asiento` → `200` `[TipoAsiento]`
#### `GET /api/tipos-asiento/{id}` → `200` `TipoAsiento` — `404` si no existe
#### `POST /api/tipos-asiento` — **ADMIN o SUPERADMIN**
```json
{ "nombre": "VIP", "multiplicador": 1.3 }
```
#### `PUT /api/tipos-asiento/{id}` — **ADMIN o SUPERADMIN**
```json
{ "nombre": "Normal", "multiplicador": 1.0 }
```
#### `DELETE /api/tipos-asiento/{id}` — **ADMIN o SUPERADMIN** → `200` body vacío

### 5.6 Asiento — `/api/asientos`

#### `GET /api/asientos` → `200` `[Asiento]`
#### `GET /api/asientos/{id}` → `200` `Asiento` — `404` si no existe
#### `POST /api/asientos` — **admin del teatro de la sala**
```json
{ "fila": "A", "numero": 1, "sala": { "id": 1 }, "tipoAsiento": { "id": 1 } }
```
`sala.id` y `tipoAsiento.id` obligatorios. `fila` es opcional (máx 10 caracteres).

#### `PUT /api/asientos/{id}` — **admin del teatro**
```json
{ "fila": "A", "numero": 1, "tipoAsiento": { "id": 1 } }
```
La `sala` no se modifica.

#### `DELETE /api/asientos/{id}` — **admin del teatro** → `200` body vacío

### 5.7 Función — `/api/funciones`

#### `GET /api/funciones` → `200` `[Funcion]` — **público**
#### `GET /api/funciones/{id}` → `200` `Funcion` — **público** — `404` si no existe
#### `GET /api/funciones/{id}/asientos` — **requiere login (CU-04)**

Devuelve todos los asientos de la sala de esa función con el campo `ocupado`:

```json
[
  {
    "id": 1,
    "fila": "A",
    "numero": 1,
    "tipoAsientoId": 2,
    "tipoAsiento": "VIP",
    "multiplicador": 1.3,
    "precio": 39000.00,
    "ocupado": false
  },
  {
    "id": 6,
    "fila": "B",
    "numero": 1,
    "tipoAsientoId": 1,
    "tipoAsiento": "Normal",
    "multiplicador": 1.0,
    "precio": 30000.00,
    "ocupado": true
  }
]
```

`ocupado` es `true` cuando existe una `Entrada` con `estado = "ACTIVA"` para esa función y ese asiento. Una entrada `CANCELADA` vuelve a dejar la butaca libre.

El `precio` de cada fila ya viene calculado: `precio base de la función × multiplicador del tipo de asiento`.

#### `POST /api/funciones` — **admin del teatro de la sala**
```json
{ "fechaHora": "2026-11-01T20:00:00", "precio": 30000.0, "obra": { "id": 1 }, "sala": { "id": 1 } }
```
`fechaHora` en ISO-8601 local. `obra.id` y `sala.id` obligatorios.

Si se omite `precio`, la función hereda el precio base de la obra. El precio de la función es un **snapshot**: cambiar el precio de la obra después no altera las funciones ya creadas.

#### `PUT /api/funciones/{id}` — **admin del teatro**
```json
{ "fechaHora": "2026-11-01T20:00:00", "precio": 30000.0, "obra": { "id": 1 }, "sala": { "id": 1 } }
```
Se pueden cambiar `fechaHora`, `precio`, `obra` y `sala`. Si se manda otra sala, se exige ser admin de ese teatro también.

#### `DELETE /api/funciones/{id}` — **admin del teatro** → `200` body vacío

### 5.8 Compra — `/api/compras`

#### `GET /api/compras` → `200` `[Compra]`
`SUPERADMIN` ve todas las compras; cualquier otro rol solo las suyas.

#### `POST /api/compras` — **cualquier usuario autenticado**
```json
{ "funcionId": 1, "asientoIds": [8, 9] }
```
Solo dos claves: `funcionId` (número) y `asientoIds` (array de números). Cualquier otra clave se ignora.

Comportamiento:
1. Valida que la función exista, que haya al menos un asiento, que no haya ids repetidos y que todos los asientos pertenezcan a la sala de la función.
2. Crea la `Compra` con `estado: "CONFIRMADA"` y total `0`.
3. Por cada asiento genera una `Entrada` con `codigo` aleatorio de 12 caracteres, `estado: "ACTIVA"` y `precioPagado = precio de la función × multiplicador del tipo de asiento`.
4. `total` y `montoTotal` = suma de esos precios.
5. `saveAllAndFlush` dentro de una transacción: si algún asiento ya estaba vendido para esa función, se aborta toda la compra y se devuelve **409**.

Ejemplo: función de una obra teatral (`$30.000`) con un asiento Normal y uno VIP:
`30.000 × 1.0 + 30.000 × 1.3 = $69.000`.

### 5.9 Entrada — `/api/entradas`

No hay endpoint de creación: las entradas se generan al comprar.

#### `GET /api/entradas` → `200` `[Entrada]`
`SUPERADMIN` ve todas; el resto solo las de sus compras.

> Este endpoint **no sirve para armar el mapa de butacas**: solo el dueño (o SuperAdmin) ve las entradas. Para eso está `GET /api/funciones/{id}/asientos`.

#### `DELETE /api/entradas/{id}` — **dueño de la compra o SUPERADMIN**
Soft delete: setea `estado: "CANCELADA"`. No borra la fila.

Esto **libera el asiento** a nivel base, porque la tabla `entrada` tiene una columna generada `asiento_si_activa` que vale `asiento_id` solo cuando `estado = 'ACTIVA'`, y un índice único sobre `(funcion_id, asiento_si_activa)`. Ese índice es exactamente lo que produce el **409** al intentar recomprar un asiento ya vendido.

### 5.10 Ticket — `/api/tickets`

Comprobante de venta. **Se genera automáticamente: uno por cada butaca comprada**, cuando la compra se confirma. No hay endpoint de creación.

El ticket guarda un *snapshot* de los datos comerciales (obra, función, teatro, sala, butaca, precio), por lo que **no cambia** aunque después se renombre la obra o se le cambie el precio. Deliberadamente **no tiene `usuario_id` ni `rol`**.

#### `GET /api/tickets/mios` → `200` `[Ticket]`
Los tickets del cliente autenticado. Como el ticket no guarda el usuario, se resuelve cruzando `ticket → entrada → compra → usuario`.

#### `GET /api/tickets` → `200` `[Ticket]`
Todos los tickets de la plataforma. Solo `SUPERADMIN`; un `CLIENTE` recibe **403**.

### 5.11 Usuario — `/api/usuarios`

#### `GET /api/usuarios` — **solo SUPERADMIN** → `200` `[Usuario]`
#### `POST /api/usuarios`
```json
{ "name": "Nuevo Admin", "email": "nuevo@example.com", "password": "clave123", "rol": "ADMIN" }
```
- `password` obligatorio y no vacío.
- `rol` opcional, default `CLIENTE`, case-sensitive.
- Si el `rol` es distinto de `CLIENTE`, exige token de `SUPERADMIN`.
- Ojo: la clave del nombre es **`name`** (igual que en register), no `nombre`.

> **No se puede crear el primer `SUPERADMIN` por API** (crear un rol no-CLIENTE exige un token de SUPERADMIN previo). Hay que insertarlo por SQL.

#### `PUT /api/usuarios/{id}` — **el propio usuario o SUPERADMIN**
```json
{ "nombre": "Lautaro", "email": "superadmin@multiteatroflores.com", "passwordHash": "clave123" }
```
El campo se llama `passwordHash` pero el valor va **en texto plano**: el controller lo hashea con BCrypt. `nombre` y `email` se escriben siempre; `passwordHash` solo si viene informado.

#### `DELETE /api/usuarios/{id}` — **solo SUPERADMIN** → `200` body vacío

---

## 6. Modelo de datos (resumen)

| Entidad | Campos | Relaciones |
|---|---|---|
| `Usuario` | `id`, `nombre`, `email`, `passwordHash`, `rol` | — |
| `Teatro` | `id`, `nombre`, `direccion` | `admin` → `Usuario` (`admin_usuario_id`) |
| `Sala` | `id`, `nombre`, `capacidad` | `teatro` → `Teatro` |
| `Asiento` | `id`, `fila`, `numero` | `sala` → `Sala`, `tipoAsiento` → `TipoAsiento` |
| `TipoAsiento` | `id`, `nombre`, `multiplicador` | — |
| `Obra` | `id`, `titulo`, `tipo`, `precio`, `clasificacion`, `descripcion`, `duracionMinutos` | — |
| `Funcion` | `id`, `fechaHora`, `precio` | `obra` → `Obra`, `sala` → `Sala` |
| `Compra` | `id`, `fecha`, `total`, `montoTotal`, `estado` | `usuario` → `Usuario` |
| `Entrada` | `id`, `codigo`, `precioPagado`, `estado` | `compra` → `Compra`, `funcion` → `Funcion`, `asiento` → `Asiento` |
| `Ticket` | `id`, `codigo`, snapshot comercial | `entrada` → `Entrada` |

## 7. Datos de ejemplo (base `multiteatro_db`)

El `script.sql` **borra y recrea las tablas** para poder correrlo varias veces. Deja siempre estos datos.

### Usuarios

| Rol | Nombre | Email | Password |
|---|---|---|---|
| SUPERADMIN | Lautaro | `superadmin@multiteatroflores.com` | `superadmin123` |
| ADMIN | Claudia Eichenberg | `claudia@noblesbestias.com` | `admin123` |
| ADMIN | Daniel Gandara | `daniel@otromundo.com` | `admin123` |
| ADMIN | Luis Enrique Cornejo | `luis@teatrohorizonte.com` | `admin123` |
| ADMIN | Gabriela Pagés | `gabriela@mandorla.com` | `admin123` |
| ADMIN | Brenda Rusconi | `brenda@teatroenlabruma.com` | `admin123` |
| ADMIN | Damián Dominguez | `damian@teatrourbano.com` | `admin123` |
| CLIENTE | Usuario1 | `usuario1@example.com` | `cliente123` |
| CLIENTE | Usuario2 | `usuario2@example.com` | `cliente123` |
| CLIENTE | Usuario3 | `usuario3@example.com` | `cliente123` |

### Teatros y salas

| Teatro | Admin | Salas |
|---|---|---|
| `1` Teatro de Las Nobles Bestias | Claudia | `1`, `2`, `3` |
| `2` Otromundo Teatro | Daniel | `4`, `5`, `6` |
| `3` Teatro Horizonte | Luis | `7`, `8`, `9` |
| `4` Mandorla Teatro | Gabriela | `10`, `11`, `12` |
| `5` Teatro en la Bruma | Brenda | `13`, `14`, `15` |
| `6` Teatro Urbano | Damián | `16`, `17`, `18` |

Cada sala tiene **25 asientos** (filas `A`-`E`, columnas `1`-`5`): fila `A` son los 5 VIP y las filas `B`-`E` son los 20 Normales.

IDs de asientos: sala 1 → `1`-`25`, sala 2 → `26`-`50`, …, sala 18 → `426`-`450`.

### Obras

| Tipo | Obras | Precio base |
|---|---|---:|
| `TEATRAL` | Toc Toc · La Función Que Sale Mal · Odisea de Florencio Varela · Barrio Privado | `$30.000` |
| `STAND_UP` | Pablo Picotto · Julian Bellese · Turco Naim | `$20.000` |
| `CONCIERTO` | Jammin · Niño Salvaje · Experiencia Beatle | `$12.500` |

### Tipos de asiento

| ID | Nombre | Multiplicador |
|---|---|---:|
| `1` | Normal | `1.0` |
| `2` | VIP | `1.3` |

### Funciones

Hay **36 funciones** (`1`-`36`): dos por cada sala. Las funciones `1`, `2`, `3`, `5`, `7`, etc. pueden usarse para probar los tres tipos de obra.

### Compras de ejemplo

| Compra | Usuario | Función | Entradas | Total |
|---|---|---|---|---|
| `1` | Usuario1 | `1` — Toc Toc (teatral) | B1 y B2 (Normal) | `$60.000` |
| `2` | Usuario2 | `2` — Pablo Picotto (stand-up) | C3 (Normal) | `$20.000` |
| `3` | Usuario3 | `8` — Jammin (concierto) | A2 (VIP) + D1, D2, D3 (Normal) | `$53.750` |

## 8. Flujo de prueba completo

1. `GET /api/obras`, `GET /api/funciones` y `GET /api/teatros` **sin token** → `200` (cartelera pública).
2. `POST /api/auth/login` como SUPERADMIN → `{{token}}`.
3. Login como `claudia@noblesbestias.com` (admin del teatro 1) → nuevo `{{token}}`.
4. `POST /api/salas` → `POST /api/asientos` (×25) → `POST /api/funciones` (2 por sala).
5. Login como `usuario1@example.com` → `{{token}}`.
6. `GET /api/funciones/1/asientos` → las butacas B1 (`id 6`) y B2 (`id 7`) deben venir con `ocupado: true`.
7. Elegir dos butacas libres, por ejemplo `8` y `9`, y ejecutar `POST /api/compras` `{"funcionId": 1, "asientoIds": [8, 9]}` → `200`.
8. Repetir con un asiento ya comprado → **409**.
9. `GET /api/entradas` para ver las propias; `DELETE /api/entradas/{id}` para liberar el asiento y ver que el mapa vuelve a mostrarlo libre.
10. Probar un admin de otro teatro modificando una sala/función ajena → **403**.
11. Probar un `id` inexistente en cualquier `GET` → **404**.

## 9. Errores frecuentes al probar

| Síntoma | Causa | Solución |
|---|---|---|
| `401` con un token que acabás de obtener | Se reinició la app | Login de nuevo (el token es en memoria) |
| `500` "Field 'clasificacion' doesn't have a default value" | Falta `clasificacion` en el body de `/api/obras` | Mandala |
| `500` con NPE al crear sala/asiento/función | Falta el `id` de la relación | Mandá `teatro` / `sala` / `obra` / `tipoAsiento` con su `id` |
| `500` por `IllegalArgumentException` en rol | Mandaste `"admin"` en minúscula | Usá `"ADMIN"` |
| `403 "No administrás este teatro"` | El `ADMIN` no es dueño de ese recurso | Usá el admin correcto o el token de SUPERADMIN |
| `404` "Obra no encontrada" (etc.) | El `id` no existe | Revisá el id o los datos de ejemplo |
| `409` al comprar | Ese asiento ya está vendido para esa función | Elegí otro, o `DELETE /api/entradas/{id}` para liberarlo |
| El navegador bloquea el fetch, Postman no | CORS | Agregá el origen real en `CorsConfig` |

## 10. Cómo levantar el proyecto

```bash
# 1. Base de datos: crear el esquema y cargar datos
#    (el script borra y recrea las tablas)
mysql -u root -e "CREATE DATABASE IF NOT EXISTS multiteatro_db;"
mysql -u root multiteatro_db < script.sql

# 2. Arrancar
./mvnw spring-boot:run
```

> Si tu MySQL no usa root sin contraseña, configurá `spring.datasource.username` y `spring.datasource.password` en `application.properties` (o en un `application-local.properties`).
