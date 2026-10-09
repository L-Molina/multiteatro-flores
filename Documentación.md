# Multiteatro API — Documentación de endpoints

- **Base URL:** `http://localhost:8080`

## 1. Autenticación

El token se obtiene con `POST /api/auth/login` y es un UUID. Si reiniciás la aplicación, los tokens emitidos dejan de funcionar y hay que hacer login de nuevo.

No hay dos tipos de token ni expiración por tiempo. El mecanismo es:

1. `POST /api/auth/login` valida email + contraseña contra el hash BCrypt en la base.
2. Si son correctos, se genera un UUID y se guarda en un `ConcurrentHashMap` (`SesionService`) asociado al usuario.
3. `ContextoSesion` lee el header en cada request, busca el UUID en el mapa y devuelve el `Usuario`. Si no lo encuentra, lanza `NoAutenticadoException` → 401.

## 2. Roles y autorización

`Usuario.rol` es un enum con tres valores: `CLIENTE`, `ADMIN`, `SUPERADMIN`.

| Regla | Comportamiento |
|---|---|
| `requiereRol(actual, ADMIN)` | Lo pasan `ADMIN` y `SUPERADMIN`. Un `CLIENTE` recibe **403**. |
| `requiereRol(actual, SUPERADMIN)` | Solo `SUPERADMIN`. |
| `requiereAdminDeTeatro(actual, teatro)` | El `ADMIN` que es dueño de ese teatro, o `SUPERADMIN`. Un admin de otro teatro recibe **403** `"No administrás este teatro"`. |

`SUPERADMIN` siempre pasa cualquier chequeo (early-return en `AutorizacionService`).

### Diagrama de permisos por endpoint

| Endpoint family | CLIENTE | ADMIN | SUPERADMIN |
|---|:---:|:---:|:---:|
| `GET` de listados y detalle (teatros, salas, obras, asientos, funciones, tipos-asiento) | ✓ | ✓ | ✓ |
| `POST/PUT/DELETE /api/obras`, `/api/tipos-asiento` | ✗ 403 | ✓ | ✓ |
| `POST/PUT/DELETE /api/teatros` (crear/borrar) | ✗ 403 | ✗ 403 | ✓ |
| `PUT/DELETE /api/teatros/{id}` (editar) | ✗ 403 | ✓ solo su teatro | ✓ |
| `POST/PUT/DELETE /api/salas`, `/api/asientos`, `/api/funciones` | ✗ 403 | ✓ solo su teatro | ✓ |
| `POST /api/compras` | ✓ | ✓ | ✓ |
| `GET /api/compras`, `GET /api/entradas` | solo las propias | solo las propias | todas |
| `GET/POST/DELETE /api/usuarios` | ✗ 403 | ✗ 403 | ✓ |
| `POST /api/usuarios` con `rol: CLIENTE` | ✓ | ✓ | ✓ |
| `PUT /api/usuarios/{id}` | solo el propio | solo el propio | cualquiera |
| `DELETE /api/entradas/{id}` | solo suyas | solo suyas | cualquiera |
| `GET/POST /api/usuarios` (crear ADMIN o SUPERADMIN) | ✗ 403 | ✗ 403 | ✓ |

---

## 3. Códigos de respuesta

| Código | Cuándo | Body |
|---|---|---|
| `200 OK` | Éxito. **Todos los writes devuelven 200**, no 201. | JSON con el recurso, o vacío en los DELETE |
| `401 Unauthorized` | Falta el header, no usa el prefijo `Bearer `, el token no existe (app reiniciada) o el login es incorrecto | Texto plano: `Falta el header Authorization o no usa el prefijo 'Bearer '` / `Token invalido o sesion expirada: volve a iniciar sesion con POST /api/auth/login` / `Credenciales inválidas` |
| `403 Forbidden` | El rol no alcanza | Texto plano: `No tenés permisos para esta acción` / `No administrás este teatro` |
| `409 Conflict` | El asiento ya fue comprado para esa función | Texto plano: `Uno de los asientos elegidos ya no está disponible para esta función` |
| `500 Internal Server Error` | Falta un campo obligatorio en el body, o un `id` no existe | Texto plano |

> Los errores son **texto plano**, no JSON.
> Un recurso inexistente devuelve **500** con `"<Entidad> no encontrado"`, no 404.

## 4. Convenciones de los bodies

- Los `id` son autogenerados: **nunca** mandarlos al crear.
- Las relaciones viajan solo con su `id`: `"teatro": {"id": 1}`, no el objeto completo.
- En los `PUT` los campos se escriben **siempre** (no hay update parcial), así que hay que mandar el objeto completo. Si se omite un campo `NOT NULL`, la request falla.
- `rol` es case-sensitive: `CLIENTE`, `ADMIN`, `SUPERADMIN`.
- `TipoAsiento` es una entidad (tabla), no un enum. `precio` es `Double`.

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
  "usuario": { "id": 1, "nombre": "Root Admin", "email": "superadmin@multiteatroflores.com",
               "passwordHash": "$2a$10$...", "rol": "SUPERADMIN" }
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

Error `500 "Faltan datos: name, email, password"` si falta alguno.

#### `POST /api/auth/logout`
Sin body. Invalida el token en el mapa de sesiones. Response `200 {"mensaje":"Sesión cerrada"}`. Después de esto, usar el mismo token devuelve **401**.

### 5.2 Teatro — `/api/teatros`

#### `GET /api/teatros` → `200` `[Teatro]`
#### `GET /api/teatros/{id}` → `200` `Teatro` — `500 "Teatro no encontrado"` si no existe
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
#### `GET /api/salas/{id}` → `200` `Sala`
#### `POST /api/salas` — **admin del teatro o SUPERADMIN**
```json
{ "nombre": "Sala 1", "capacidad": 120, "teatro": { "id": 1 } }
```
`teatro.id` obligatorio (si falta, NPE → 500).

#### `PUT /api/salas/{id}` — **admin del teatro de esa sala**
```json
{ "nombre": "Sala 1", "capacidad": 120 }
```
El `teatro` no se modifica.

#### `DELETE /api/salas/{id}` — **admin del teatro** → `200` body vacío

### 5.4 Obra — `/api/obras`

#### `GET /api/obras` → `200` `[Obra]`
#### `GET /api/obras/{id}` → `200` `Obra`
#### `POST /api/obras` — **ADMIN o SUPERADMIN**
```json
{ "titulo": "Hamlet", "clasificacion": "+13", "descripcion": "Tragedia de Shakespeare.", "duracionMinutos": 150 }
```
`titulo`, `clasificacion` y `duracionMinutos` son obligatorios. `clasificacion` es texto libre de hasta 20 caracteres (por edad: `ATP`, `+13`, `+18`); si se omite, la columna `NOT NULL` hace fallar la request con **500**.

#### `PUT /api/obras/{id}` — **ADMIN o SUPERADMIN**
```json
{ "titulo": "Hamlet", "clasificacion": "+13", "descripcion": "Tragedia de Shakespeare.", "duracionMinutos": 150 }
```
Los cuatro campos se escriben siempre: mandá el objeto completo.

#### `DELETE /api/obras/{id}` — **ADMIN o SUPERADMIN** → `200` body vacío


### 5.5 Tipo de asiento — `/api/tipos-asiento`

Path con guion, `tipos-asiento`.

#### `GET /api/tipos-asiento` → `200` `[TipoAsiento]`
#### `GET /api/tipos-asiento/{id}` → `200` `TipoAsiento`
#### `POST /api/tipos-asiento` — **ADMIN o SUPERADMIN**
```json
{ "nombre": "Platea", "precio": 12000.0 }
```
#### `PUT /api/tipos-asiento/{id}` — **ADMIN o SUPERADMIN**
```json
{ "nombre": "Platea", "precio": 12000.0 }
```
#### `DELETE /api/tipos-asiento/{id}` — **ADMIN o SUPERADMIN** → `200` body vacío

### 5.6 Asiento — `/api/asientos`

#### `GET /api/asientos` → `200` `[Asiento]`
#### `GET /api/asientos/{id}` → `200` `Asiento`
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

#### `GET /api/funciones` → `200` `[Funcion]`
#### `GET /api/funciones/{id}` → `200` `Funcion`
#### `POST /api/funciones` — **admin del teatro de la sala**
```json
{ "fechaHora": "2026-10-01T20:00:00", "precio": 5000.0, "obra": { "id": 1 }, "sala": { "id": 1 } }
```
`fechaHora` en ISO-8601 local. `obra.id` y `sala.id` obligatorios.

> El `precio` de la función **no interviene en el cobro**: el importe de una compra sale del `precio` del `TipoAsiento` de cada asiento.

#### `PUT /api/funciones/{id}` — **admin del teatro**
```json
{ "fechaHora": "2026-10-01T20:00:00", "precio": 5000.0, "sala": { "id": 1 } }
```
La `obra` no se modifica. Si se manda otra `sala`, se exige ser admin de ese teatro también.

#### `DELETE /api/funciones/{id}` — **admin del teatro** → `200` body vacío

### 5.8 Compra — `/api/compras`

#### `GET /api/compras` → `200` `[Compra]`
`SUPERADMIN` ve todas las compras; cualquier otro rol solo las suyas.

#### `POST /api/compras` — **cualquier usuario autenticado**
```json
{ "funcionId": 1, "asientoIds": [4, 6] }
```
Solo dos claves: `funcionId` (número) y `asientoIds` (array de números). Cualquier otra clave se ignora.

Comportamiento:
1. Crea la `Compra` con `estado: "CONFIRMADA"` y total `0`.
2. Por cada asiento genera una `Entrada` con `codigo` aleatorio de 12 caracteres, `estado: "ACTIVA"` y `precioPagado` = precio del `TipoAsiento` de ese asiento.
3. `total` y `montoTotal` = suma de esos precios.
4. `saveAllAndFlush` dentro de una transacción: si algún asiento ya estaba vendido para esa función, se aborta toda la compra y se devuelve 409.

Response `200`:
```json
{ "id": 6, "fecha": "2026-09-28T00:12:00", "estado": "CONFIRMADA",
  "total": 13500.0, "montoTotal": 13500.0, "usuario": { "id": 4, "..." : "..." } }
```
No incluye las entradas generadas; se consultan con `GET /api/entradas`.

### 5.9 Entrada — `/api/entradas`

No hay endpoint de creación: las entradas se generan al comprar.

#### `GET /api/entradas` → `200` `[Entrada]`
`SUPERADMIN` ve todas; el resto solo las de sus compras.

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

```json
{
  "id": 1,
  "codigo": "TCK-6B6363C8-9",
  "fechaEmision": "2026-10-01T20:52:57",
  "funcionFechaHora": "2026-10-03T20:00:00",
  "obraTitulo": "El Enfermo Imaginario",
  "obraClasificacion": "ATP",
  "teatroNombre": "Teatro Lanús",
  "salaNombre": "Sala Principal",
  "butacaFila": "B",
  "butacaNumero": 1,
  "precioPagado": 5000.00
}
```

**Mail de confirmación.** Al confirmarse la compra se envía un mail al usuario con el detalle de sus tickets. Ocurre **después del commit** de la transacción: si la compra falla (por ejemplo, 409 por asiento ya vendido), **no se genera ningún ticket ni se envía ningún mail**. Con `mail.enabled=false` el mail se imprime en el log en vez de enviarse.

### 5.11 Usuario — `/api/usuarios`

#### `GET /api/usuarios` — **solo SUPERADMIN** → `200` `[Usuario]`
#### `POST /api/usuarios`
```json
{ "name": "Nuevo Admin", "email": "nuevo@example.com", "password": "clave123", "rol": "ADMIN" }
```
- `password` obligatorio y no vacío → si falta, `500 "Falta password"`.
- `rol` opcional, default `CLIENTE`, case-sensitive.
- Si el `rol` es distinto de `CLIENTE`, exige token de `SUPERADMIN`.
- Ojo: la clave del nombre es **`name`** (igual que en register), no `nombre`.

> **No se puede crear el primer `SUPERADMIN` por API** (crear un rol no-CLIENTE exige un token de SUPERADMIN previo). Hay que insertarlo por SQL.

#### `PUT /api/usuarios/{id}` — **el propio usuario o SUPERADMIN**
```json
{ "nombre": "Lautaro Molina", "email": "lautaro@example.com", "passwordHash": "clave123" }
```
El campo se llama `passwordHash` pero el valor va **en texto plano**: el controller lo hashea con BCrypt. `nombre` y `email` se escriben siempre (mandalos o los borrás); `passwordHash` solo si viene informado.

Error `403 "Solo el propio usuario o el SuperAdmin puede editar este perfil"` si intentás editar el perfil de otro.

#### `DELETE /api/usuarios/{id}` — **solo SUPERADMIN** → `200` body vacío

---

## 6. Modelo de datos (resumen)

| Entidad | Campos | Relaciones |
|---|---|---|
| `Usuario` | `id`, `nombre`, `email`, `passwordHash`, `rol` | — |
| `Teatro` | `id`, `nombre`, `direccion` | `admin` → `Usuario` (`admin_usuario_id`) |
| `Sala` | `id`, `nombre`, `capacidad` | `teatro` → `Teatro` |
| `Asiento` | `id`, `fila`, `numero` | `sala` → `Sala`, `tipoAsiento` → `TipoAsiento` |
| `TipoAsiento` | `id`, `nombre`, `precio` | — |
| `Obra` | `id`, `titulo`, `clasificacion`, `descripcion`, `duracionMinutos` | — |
| `Funcion` | `id`, `fechaHora`, `precio` | `obra` → `Obra`, `sala` → `Sala` |
| `Compra` | `id`, `fecha`, `total`, `montoTotal`, `estado` | `usuario` → `Usuario` |
| `Entrada` | `id`, `codigo`, `precioPagado`, `estado` | `compra` → `Compra`, `funcion` → `Funcion`, `asiento` → `Asiento` |

Las respuestas JSON incluyen `passwordHash` (bcrypt) en los objetos `Usuario`: no es información sensible utilizable, pero conviene saberlo.

## 7. Datos de ejemplo (base `multiteatro_db`)

| **Teatros** | `1` Teatro Lanús (admin = usuario 2) · `2` Teatro Banfield (admin = usuario 3) |
| **Salas** | `1` Sala Principal (teatro 1, cap. 12) · `2` Sala Intima (teatro 2, cap. 8) |
| **Obras** | `1` El Enfermo Imaginario (ATP) · `2` Un Tranvía Llamado Deseo (+13) |
| **Tipos de asiento** | `1` Standard (5000) · `2` VIP (8500) |
| **Funciones** | `1` y `2` en sala 1 · `3` en sala 2 |
| **Asientos** | sala 1 → ids `1`-`12`; sala 2 → ids `13`-`20` |

Usuarios de prueba:

| Rol | Email | Password |
| SUPERADMIN | `superadmin@multiteatroflores.com` | `superadmin123` |
| ADMIN (teatro 1) | `marta@teatrolanus.com` | `admin123` |
| ADMIN (teatro 2) | `carlos@teatrobanfield.com` | `admin123` |
| CLIENTE | `lautaro@example.com` | `cliente123` |
| CLIENTE | `ana@example.com` | `cliente123` |

## 8. Flujo de prueba completo

1. `POST /api/auth/login` como SUPERADMIN → `{{token}}`.
2. `POST /api/obras` y `POST /api/tipos-asiento` (crear catálogo).
3. `POST /api/teatros` con `"admin": {"id": 2}`.
4. Login como `marta@teatrolanus.com` (admin del teatro 1) → nuevo `{{token}}`.
5. `POST /api/salas` → `POST /api/asientos` (×N) → `POST /api/funciones`.
6. Login como `lautaro@example.com` (cliente) → `{{token}}`.
7. `POST /api/compras` `{"funcionId": 1, "asientoIds": [4, 6]}` → `200`.
8. Repetir con un asiento ya comprado → **409**.
9. `GET /api/entradas` para verlas; `DELETE /api/entradas/{id}` para liberar el asiento.

## 9. Errores frecuentes al probar

| Síntoma | Causa | Solución |
| `401` con un token que acabás de obtener | Se reinició la app | Login de nuevo (el token es en memoria) |
| `500` "Field 'clasificacion' doesn't have a default value" | Falta `clasificacion` en el body de `/api/obras` | Mandala |
| `500` con NPE al crear sala/asiento/función | Falta el `id` de la relación | Mandá `teatro` / `sala` / `obra` / `tipoAsiento` con su `id` |
| `500` "Falta password" | `POST /api/usuarios` sin `password` | Mandalo |
| `500` por `IllegalArgumentException` en rol | Mandaste `"admin"` en minúscula | Usá `"ADMIN"` |
| `403 "No administrás este teatro"` | El `ADMIN` no es dueño de ese recurso | Usá el admin correcto o el token de SUPERADMIN |
| `409` al comprar | Ese asiento ya está vendido para esa función | Elegí otro, o `DELETE /api/entradas/{id}` para liberarlo |

## 10. Cómo levantar el proyecto

```bash
# 1. Base de datos: crear el esquema y cargar datos
mysql -u root -e "CREATE DATABASE multiteatro_db;"
mysql -u root multiteatro_db < script.sql

# 2. Arrancar
./mvnw spring-boot:run
```