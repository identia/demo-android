SDK Identia Flow - Android</h1>

## Descripción

El SDK Identia Flow tiene como propósito capturar una selfie y ambos lados de un documento de identidad para verificar la identidad del usuario. 

## Cómo Integrar el SDK

### Configuración del Gradle

Añade la dependencia y el repositorio maven en el archivo `build.gradle` de tu módulo:

```kotlin
dependencies {
    implementation 'identia:flow:0.0.3'
}

repositories {
    maven {
        url "https://repo.repsy.io/mvn/identia/identia"
    }
}
```

### Importaciones necesarias

En tu actividad o fragmento donde desees utilizar el SDK, asegúrate de importar:

```kotlin
import pe.identia.flow.FlowActivity
```

### Actualización del AndroidManifest.xml

Añade la actividad `FlowActivity` a tu `AndroidManifest.xml`:

```xml
<activity android:name="pe.identia.flow.FlowActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="pe.identia.camera.OPEN_FLOW" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

### Obtención del Token de Acceso desde el Backend

Es esencial que la obtención del token de acceso se realice en el backend de tu aplicación para no exponer las credenciales. Una vez que tu backend haya obtenido el token, puede enviarlo al frontend (tu aplicación Android) de manera segura para que pueda ser utilizado en solicitudes subsiguientes al servidor.

### Uso del SDK

Una vez que hayas recibido el token de acceso desde tu backend en tu aplicación Android, puedes iniciar un proceso de identificación.

Crea un Intent para `FlowActivity` y pasa los datos necesarios como extras:

```kotlin
val intent = Intent(this@MainActivity, FlowActivity::class.java)
intent.putExtra("idSession", YOUR_OBTAINED_ID)  // Corregido a "idSession" para claridad.
intent.putExtra("endPoint", "https://apifacialdev.identia.pe/")

if (intent.resolveActivity(packageManager) != null) {
    startActivity(intent)
} else {
    // Maneja el caso en el que no se encuentra la actividad
}
```

### Manejo de Respuestas

Utiliza el `ActivityResultLauncher` para recibir y manejar las respuestas de `FlowActivity`.

```kotlin
resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    val jsonResponse = JSONObject(result.data?.getStringExtra("response"))
    if (jsonResponse.getBoolean("result")) {
        // Procesa respuestas exitosas aquí
    } else {
        // Maneja errores aquí
    }
}
```

El SDK de Identia Flow retornará respuestas que tu aplicación debe manejar adecuadamente para proporcionar retroalimentación al usuario:

#### Respuestas Exitosas:

Cuando el proceso concluye de manera exitosa, recibirás la siguiente estructura:

```json
{
    "result": true,
    "message": "El proceso ha concluido satisfactoriamente",
    "code": 200,
    "data": {
        "selfie": "BASE64_DATA_FOR_SELFIE",
        "document": {
            "A": "BASE64_DATA_FOR_SIDE_A",
            "B": "BASE64_DATA_FOR_SIDE_B"
        }
    }
}
```

Con esta respuesta, puedes extraer y procesar la selfie y la imagenes del documento según sea necesario en tu aplicación.

#### Errores por verificación negativa

```json
{
    "result": false,
    "message": "Proceso de verificación concluido, sin respuesta satisfactoria",
    "code": 300,
    "data": null
}
```


#### Errores por falta de permisos:

Si el usuario no otorga permisos para acceder a la cámara, recibirás la siguiente respuesta:

```json
{
    "result": false,
    "message": "Por favor, otorga permisos de cámara para continuar.",
    "code": 401,
    "data": null
}
```

Es importante informar al usuario sobre la necesidad de otorgar estos permisos para continuar con el proceso.

#### Errores por timeout:

Si el usuario excede el tiempo máximo permitido para capturar una selfie o el DNI, recibirás esta respuesta:

```json
{
    "result": false,
    "message": "Se agotó el tiempo de espera. Por favor, intenta de nuevo más tarde.",
    "code": 408,
    "data": null
}
```

Debes informar al usuario sobre el tiempo agotado y considerar darle opciones para reiniciar el proceso o intentarlo más tarde.

