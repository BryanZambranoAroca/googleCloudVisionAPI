package com.example.googlecloudvisionapi;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.TextAnnotation;

import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity{

    static final int REQUEST_IMAGE_CAPTURE = 1;
    ImageView imagen ;
    public Vision vision;
    TextView txtDatos;
    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imagen = (ImageView) findViewById(R.id.imvImagen);



        lstOpciones=(ListView) findViewById(R.id.lstOpciones);
        mostrarListViewMultipleChoiceChecked();


        //Configuracion de la barra scroll de desplazamaiento para el control TextView
        txtDatos = findViewById(R.id.txtDatos);
        txtDatos.setMovementMethod(new ScrollingMovementMethod());


        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("API KEY"))
                 vision = visionBuilder.build();



    }

    ListView lstOpciones;
    private void mostrarListViewMultipleChoiceChecked() {
        final String[] lista =
                new String[]{"LABEL_DETECTION", "FACE_DETECTION", "TEXT_DETECTION"};

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice, lista);

        lstOpciones.setAdapter(arrayAdapter);

    }

    // procedimiento no retorna  nada
    //funcion o metodo retorna algo
    String message;
    List<String> opcionesSelec;
    public void ejecutarOpcion(View view){


         opcionesSelec = new ArrayList<>();
        long[] idItemsCheck = lstOpciones.getCheckItemIds();
        for (int i = 0; i < idItemsCheck.length; i++) {
            opcionesSelec.add(lstOpciones.getItemAtPosition((int) idItemsCheck[i]).toString());
        }

        if (opcionesSelec.size() > 0) {
            txtDatos.setText("");
            message="";

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                bitmap = scaleBitmapDown(bitmap, 1200);
                ByteArrayOutputStream stream = new ByteArrayOutputStream(); //2da de la api
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] imageInByte = stream.toByteArray();
                Image inputImage = new Image(); //googlevision
                inputImage.encodeContent(imageInByte);


                //Armo mi listado de solicitudes
                List<Feature> desiredFeaturelst = new ArrayList<>();

                //Realizar la solicitud de cualquier tipo de los servicio que ofrece la API
                Feature desiredFeatureitem;


                //Recorro mi listado de solicitudes seleccionadas
                    for (int i = 0; i < opcionesSelec.size(); i++) {
                        desiredFeatureitem = new Feature();
                        desiredFeatureitem.setType(opcionesSelec.get(i));

                        //Cargo a mi lista la solicitud
                        desiredFeaturelst.add(desiredFeatureitem);
                    }

                    //Armamos la solicitud o las solicitudes .- FaceDetection solo o facedeteccion,textdetection,etc..
                    AnnotateImageRequest request = new AnnotateImageRequest();
                    request.setImage(inputImage);
                    request.setFeatures(desiredFeaturelst);
                    BatchAnnotateImagesRequest batchRequest = new
                            BatchAnnotateImagesRequest();
                    batchRequest.setRequests(Arrays.asList(request));

                    //Asignamos al control VisionBuilder la solicitud
                    BatchAnnotateImagesResponse batchResponse = null;
                    try {
                        Vision.Images.Annotate annotateRequest =
                                vision.images().annotate(batchRequest);
                        //Enviamos la solicitud
                        annotateRequest.setDisableGZipContent(true);
                        batchResponse = annotateRequest.execute();
                    } catch (IOException ex) {
                        message+=ex.getMessage()+"\n";
                        //Toast.makeText(MainActivity.this, ex.getMessage().toString(), Toast.LENGTH_SHORT).show();
                    }


                    if(batchResponse!=null) {
                        //Tratar la respuesta detección de texto
                        //TextAnnotation text =batchResponse.getResponses().get(0).getFullTextAnnotation();

                        for (int i = 0; i < opcionesSelec.size(); i++) {
                            switch (opcionesSelec.get(i)) {
                                case "LABEL_DETECTION":
                                    message += "LABEL_DETECTION" + "\n";
                                    StringBuilder messagebuilder = new StringBuilder("I found these things:\n\n");
                                    List<EntityAnnotation> labels =
                                            batchResponse.getResponses().get(0).getLabelAnnotations();
                                    if (labels != null) {
                                        for (EntityAnnotation label : labels) {
                                            messagebuilder.append(String.format(Locale.US, "%.3f: %s",
                                                    label.getScore(), label.getDescription()));
                                            messagebuilder.append("\n");
                                        }
                                    } else {
                                        messagebuilder.append("Nothing");
                                    }
                                    message += messagebuilder.toString() + "\n";
                                    break;

                                case "FACE_DETECTION":
                                    message += "FACE_DETECTION" + "\n";
                                    //Deteccón de rostros
                                    List<FaceAnnotation> faces = batchResponse.getResponses().get(0).getFaceAnnotations();
                                    if (faces != null) {
                                        int numberOfFaces = faces.size();
                                        String likelihoods = "";
                                        for (int j = 0; j < numberOfFaces; j++) {
                                            //probabilidad de alegria
                                            likelihoods += "\n it is " + faces.get(j).getJoyLikelihood() + " that face " + j + " is happy";
                                        }
                                        message += "This photo has " + numberOfFaces + " faces" + likelihoods + "\n";
                                    } else {
                                        message += "This picture doesn't contains faces" + "\n";
                                    }
                                    break;

                                case "TEXT_DETECTION":
                                    message += "TEXT_DETECTION" + "\n";
                                    TextAnnotation text = batchResponse.getResponses().get(0).getFullTextAnnotation();
                                    if (text != null) {
                                        message += text.getText() + "\n";
                                    } else {
                                        message += "This picture doesn't contain text" + "\n";
                                    }

                                    break;
                            }
                        }
                    }
                    else{

                    }


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtDatos.setText(message);
                        }
                    });
            }
        });
        }
        else
        {
            Toast.makeText(MainActivity.this,"Seleccione una opción por favor",Toast.LENGTH_SHORT).show();
        }

    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private void tomarFoto(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }

    }

    public void selecImg(View v)
    {

        final CharSequence[] opciones ={"Tomar Foto","Buscar Imagen"};
        final AlertDialog.Builder alertOpciones = new AlertDialog.Builder(MainActivity.this);
        alertOpciones.setTitle("Seleccione...");
        alertOpciones.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (opciones[i].toString()){
                    case "Tomar Foto":
                        tomarFoto();
                        break;

                    case "Buscar Imagen":
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/");
                        startActivityForResult(intent.createChooser(intent,"Seleccione la app"),10);
                        break;
                }
            }
        });
        alertOpciones.show();

    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode==RESULT_OK){
            switch(requestCode){
                case 10:
                    Uri MIpath = data.getData();
                    imagen.setImageURI(MIpath);
                    break;
                case 1:
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imagen.setImageBitmap(imageBitmap);

                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + requestCode);
            }

        }

    }



}
