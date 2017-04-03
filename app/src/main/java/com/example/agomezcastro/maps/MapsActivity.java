package com.example.agomezcastro.maps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Circle circle;
    private MapsFragment mMapsFragment;
    private static final int PERMISO = 1;

    //Coordenadas del usuario //Distancia entre el usuario y la marca
    double latitud, longitud, distanciaEntrePuntos;

    //Coordenadas del sitio donde está puesta la marca
    double latMark = 42.2370411;
    double lngMark = -8.718259900000021;

    //Instrucciones del juego
    String instrucciones = "En este mapa hay una marca que no se ve...\n" +
            "Pero no te preocupes! Como verás, el mapa está " +
            "delimitado. Es dentro de esa zona donde " +
            "tendrás que buscar la marca. La zona, irá cambiando de color:\n" +
            "\tRojo: Aonde vaaaaaaassss!!!\n" +
            "\tNaranja: Ma o menos\n" +
            "\tAmarillo: Al menos estas encaminado\n" +
            "\tVerde: Estás ciego? Lo tienes casi en tus narices!!!\n" +
            "Preparado? Ea! A correr!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //Creación fragmento de forma dinámica
        mMapsFragment = MapsFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, mMapsFragment)
                .commit();
        mMapsFragment.getMapAsync(this);//Asocia la actividad como escucha
        obtenerUbicacion();
        crearDialogoInstrucciones();
    }

    /**
     * Este método es llamado cuando el mapa está listo para ser utilizado
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        solicitarPermiso();
        mMap.getUiSettings().setZoomControlsEnabled(true); //Controles integrados de zoom
        mMap.getUiSettings().setCompassEnabled(true); //Brújula
        crearZonaDelimitada();
    }

    /***********************OBTENCIÓN DE UBICACIÓN A TRAVÉS DE LOCATION MANAGER********************/
    /**
     * Obtiene la posición actual del usuario
     */
    public void obtenerUbicacion(){
        LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locLis);
    }

    LocationListener locLis= new LocationListener() {
        /**
         * Se ejecuta cada vez que el GPS recibe nuevas coordenadas
         * debido a la detección de un cambio de ubicación
         * @param loc
         */
        @Override
        public void onLocationChanged(Location loc) {
            //LatLng miPosicion = new LatLng(loc.getLatitude(), loc.getLongitude());
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 16));
            compararDistancia(loc);
            calcularDistancia(loc);

        }

        /**
         * Se ejecuta cada vez que se detecta un cambio en el
         * status del proveedor de localización (GPS)
         * @param provider
         * @param status
         * @param extras
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Este metodo se ejecuta cada vez que se detecta un cambio en el
            // status del proveedor de localizacion (GPS)
            // Los diferentes Status son:
            // OUT_OF_SERVICE -> Si el proveedor esta fuera de servicio
            // TEMPORARILY_UNAVAILABLE -> Temporalmente no disponible pero se
            // espera que este disponible en breve
            // AVAILABLE -> Disponible
        }

        /**
         * Se ejecuta cuando el GPS está activado
         * @param provider
         */
        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "GPS activado",
                    Toast.LENGTH_SHORT).show();
        }

        /**
         * Se ejecuta cuando el GPS está desactivado
         * @param provider
         */
        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "GPS desactivado",
                    Toast.LENGTH_SHORT).show();
        }
    };
    /********************************FIN OBTENCIÓN DE UBICACIÓN************************************/

    /**
     * Calcula la distancia entre la posición actual del usuario
     * y el sitio al que se tiene que llegar
     * @param loc
     * @return Retorna la distancia entre los dos puntos.
     */
    public double calcularDistancia(Location loc) {
        latitud = loc.getLatitude();
        longitud = loc.getLongitude();
        Location myLocation = new Location("Mi localización");
        myLocation.setLatitude(latitud);
        myLocation.setLongitude(longitud);
        //myLocation.setLatitude(42.2366001);
        //myLocation.setLongitude(-8.71455160000005);
        Location markerLocation = new Location("Localización marca");
        //markerLocation.setLatitude(37.4234331);
        //markerLocation.setLongitude(-122.08681530000001);
        markerLocation.setLatitude(latMark);
        markerLocation.setLongitude(lngMark);
        distanciaEntrePuntos = myLocation.distanceTo(markerLocation);

        return distanciaEntrePuntos;
    }

    /**
     * Compara la distancia entre 20 (en metros). Si la distancia es menor o igual
     * a 20 se mostrará la marca de dicho lugar.
     * @param loc
     */
    public void compararDistancia(Location loc){
        double distancia = calcularDistancia(loc);
        if(distancia <= 20.00){
            crearMarca();
        }
        cambiarColorZonaDelimitada(distancia);
    }

    /**
     * Crea la marca del lugar al que se tiene que llegar
     */
    public void crearMarca(){
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latMark, lngMark))
                .title("Aquí estoy!!"));
    }

    /**
     * Delimita la zona en la que hay que buscar la marca dentro de un círculo
     */
    public void crearZonaDelimitada(){
        LatLng center = new LatLng(42.2367671, -8.71800010000004);
        int radius = 100;
        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeWidth(4);
        circle = mMap.addCircle(circleOptions);
    }

    /**
     * Dependiendo de la distancia a la que esté el usuario de la marca,
     * la zona delimitada cambiará de color.(Rojo, naranja, amarillo o verde)
     * @param distance Distancia a la que se encuentra el usuario de la marca
     */
    public void cambiarColorZonaDelimitada(double distance){
        if(distance >= 100.00){
            circle.setFillColor(Color.argb(75, 255, 51, 51));
            circle.setStrokeColor(Color.parseColor("#660000"));
        }
        if(distance >= 75.00 && distance < 100.00){
            circle.setFillColor(Color.argb(75, 255, 128, 0));
            circle.setStrokeColor(Color.parseColor("#FF8000"));
        }
        if(distance < 75.00 && distance > 50.00){
            circle.setFillColor(Color.argb(75, 255, 255, 0));
            circle.setStrokeColor(Color.parseColor("#FFFF00"));
        }
        if(distance <=50.00) {
            circle.setFillColor(Color.argb(75, 76, 153, 0));
            circle.setStrokeColor(Color.parseColor("#1ea00d"));
        }
    }

    /**************************************CREACIÓN DE MENÚ****************************************/
    /**
     * Método autoinvocado para inflar el código XML (menu_main.xml)
     * @param menu Instancia del elemento <menú>
     * @return Devuelte un true para indicar que la acción ha sido exitosa
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Método autoinvocado cuando el usuario presiona un botón.
     * Para saber que botón ha sido presionado se obtiene su id
     * con el método getItemId().
     * @param item Instancia del nodo <item>
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.instrucciones:
                crearDialogoInstrucciones();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Crea un AlertDialog con las instrucciones del juego
     */
    public void crearDialogoInstrucciones(){
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        build.setTitle("Encuentra la marca oculta!!");
        build.setMessage(instrucciones);
        build.setPositiveButton("Aceptar",null);
        build.create();
        build.show();
    }
    /**************************************FIN CREACIÓN MENÚ***************************************/

    /***********************************PERMISOS DE LOCALIZACIÓN***********************************/
    /**
     * Comprueba los permisos y los concede
     * Muestra un mensaje en tiempo de ejecución explicando la necesidad de dichos permisos
     */
    public void solicitarPermiso(){
        int checkPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (checkPerm == PackageManager.PERMISSION_GRANTED) { //Concede el permiso para la localización
            mMap.setMyLocationEnabled(true);
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Aquí se muestra el diálogo explicativo de porque se necesitan los permisos
            } else {
                // Se solicitan los permisos
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISO);
            }
        }
    }

    /**
     * Devuelve la llamada para el resultado de solicitar permisos.
     * Se invoca a través del método requestPermission()
     * @param requestCode Código de solicitud devuelto por requestPermission()
     * @param permissions Permisos solicitados. Nunca puede ser nulo
     * @param grantResults Resultado de la conexión para los permisos
     *                     correspondientes.(PERMISSION_GRANTED o PERMISSION_DENIED)Nunca nulo
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISO) {
            // ¿Permisos asignados?
            if (permissions.length > 0 &&
                    permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                /*
                Al habilitar la capa MyLocation, aparece en la parte superior derecha un botón de localización.
                Al pulsarlo, la cámara centra el mapa en la ubicación actual.
                La ubicación se muestra a través de un punto azul.
                 */
                mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(this, "Error de permisos", Toast.LENGTH_LONG).show();
            }
        }
    }
    /*******************************FIN PERMISOS LOCALIZACIÓN**************************************/
}