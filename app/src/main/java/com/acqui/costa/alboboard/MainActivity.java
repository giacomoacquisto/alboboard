package com.acqui.costa.alboboard;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;
import com.example.manuel.keasy2.R;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private View itemView;
    private ArrayList<com.acqui.costa.alboboard.Character> characters = new ArrayList<>();
    private int count=0;
    private RelativeLayout l;
    private EditText anteprima;
    private TextView testo;
    private Dialog dialog;
    private Dialog dialogGuide;
    private int offset_t=0;
    private int offset_l=0;
    /*
    * FocusFlag
     * è un flag che serve per capire se si sta modificando
     * o aggiungendo un carattere
     * true se si sta modificando
     * false se si sta aggiungendo un nuovo carattere
    */
    private Boolean focusFlag=false;
    private long mLastClickTime = 0;
    private static Map<String, Integer> tagAnteprima = new HashMap<>();
    private Button modify;
    private  int coloreSfondo ;
    private Boolean nonMostrareFlag=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        l= (RelativeLayout) findViewById(R.id.box);

        final RelativeLayout primoLivello= (RelativeLayout) findViewById(R.id.primo_livello); //Mi serve per cambiare lo sfondo

        findViewById(R.id.deleteZone).setOnDragListener(new DragListeneDelete(characters)); //Drag listener per elminare i caratteri

        Button add= (Button) findViewById(R.id.buttonAdd);
        final View deleteIcon= findViewById(R.id.deleteZone);
        Button done= (Button) findViewById(R.id.buttonDone);
        modify = (Button)findViewById(R.id.buttonModify);
        final Button setting= (Button) findViewById(R.id.buttonSetting);
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        final LayoutInflater inflater= LayoutInflater.from(this);

        assert l != null;
        l.setOnDragListener(new DragListener(characters));


        //Ripristino preferenze guida
        nonMostrareFlag = sharedPref.getBoolean("nonMostrareFlag", true);



        //Ripristino ultimo colore sfondo utilizzato l'ultima volta
        coloreSfondo= sharedPref.getInt("BackgroundColor", Color.parseColor("#536dfe"));
        primoLivello.setBackgroundColor(coloreSfondo);

        //Serve per la forma e per il colore iniziale dell'anteprima
        GradientDrawable gradientAnteprima =new GradientDrawable();
        gradientAnteprima.setShape(GradientDrawable.RECTANGLE);
        gradientAnteprima.setCornerRadius(10.f);

        //leggo la stringa json dell'impostazione precedente
        lastSetupToCharacters();

        assert done != null;
        /*Click sul tasto di fine configurazione
        * creo un nuovo intent
        * associo l'array di oggetti Characters
        * lancio l'intent
        */
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("PROVA", "DONE BUTTON");
                Intent mIntent = new Intent(MainActivity.this, SpiritTableActivity.class);
                mIntent.putParcelableArrayListExtra("characters", characters);
                startActivity(mIntent);
                finish();
            }
        });

        //Controllo se provengo dalla spirit table
        Boolean noStart= sharedPref.getBoolean("noStart", false);
        if(noStart == false){
            //rimando all'altra activity
            done.performClick();
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("noStart", false);
        editor.apply();

        //visualizzo i caratteri letti dal setup precedente
        for (com.acqui.costa.alboboard.Character ch: characters) {
            itemView = inflater.inflate(R.layout.item, l, false);
            itemView.setOnLongClickListener(new LongPressListener(deleteIcon));  //Associo un longPressListener
            itemView.setFocusableInTouchMode(true);

            itemView.setOnFocusChangeListener(onFocusChangeListenerCharacter); //gestisco la comparsa e scomparsa del tasto di modifica

            TextView newCh = (TextView) itemView.findViewById(R.id.character); //carattere che andrà nel box
            ch.copyCharacterToTextView(newCh);
            l.addView(itemView);

            if ((count)< (ch.getId()+1))    //Per l'ID crescente
                    count= ch.getId()+1;
        }

        //TODO Guida introduttiva
        dialogGuide = new Dialog(this);
        dialogGuide.setContentView(R.layout.guida);
        dialogGuide.setCanceledOnTouchOutside(false);

        dialogGuide.setTitle("Guida");
        Button doneBtn = (Button) dialogGuide.findViewById(R.id.button_done);

        final CheckBox checkBox = (CheckBox) dialogGuide.findViewById(R.id.checkBox);

        testo = (TextView) dialogGuide.findViewById(R.id.testo);
        testo.setText("\nBenvenuto!\n\n" +
                "Premi il tasto + per inserire una lettera.\n\n" +
                "Premi il tasto in alto a destra per aprire il menù nel quale puoi:\n" +
                "-Cambiare il colore dello sfondo;\n" +
                "-Salvare e ripristinare ed eliminarela tua configurazione;\n" +
                "-Cancellare tutto" +
                "-Attivare la guida.\n");

        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogGuide.dismiss();
            }
        });
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                nonMostrareFlag=!isChecked;
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("nonMostrareFlag", nonMostrareFlag);
                editor.apply();
            }
        });
        if (nonMostrareFlag)
            dialogGuide.show();
        /**fine gestione guida.xml*/

        //TODO preparo la dialog inserimento lettera
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog);
        dialog.setTitle("Nuovo Carattere");

        anteprima= (EditText) dialog.findViewById(R.id.anteprima) ;
        gradientAnteprima.setColor(Color.WHITE);
        tagAnteprima.put("color", Color.WHITE);  //inserisco il colore nel tag(Ne tengo traccia)

        anteprima.setBackgroundDrawable(gradientAnteprima); //setto il colore dell'anteprima graficamente
        anteprima.setTag(tagAnteprima);  //sett il tag

        Button buttonColor= (Button) dialog.findViewById(R.id.buttonColor);
        Button conferma= (Button) dialog.findViewById(R.id.okButton);
        ZoomControls zoomControls= (ZoomControls) dialog.findViewById(R.id.zoomControls);


        //gestisco il colorPicker
        final ColorPickerDialog colorPickerDialog = new ColorPickerDialog();
        final ColorPickerDialog colorPickerDialogSfondo = new ColorPickerDialog();

        //Prendo i colori dalla risorsa color
        TypedArray ta = getResources().obtainTypedArray(R.array.colors);
        int[] colors = new int[ta.length()];
        for (int i = 0; i < ta.length(); i++) {
            colors[i] = ta.getColor(i, 0);
        }
        ta.recycle();
        //inizializzo il color picker
        colorPickerDialog.initialize(R.string.title, colors, 424242, 8, colors.length);
        colorPickerDialogSfondo.initialize(R.string.title, colors, 424242, 8, colors.length);

        assert add != null;
        //mostra la finestra di dialog per aggiungere un carattere
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //TODO Guida tasto +
                testo.setText(
                        "\nPremere sulla linea lampeggiante per inserire un carattere.\n\n" +
                        "Premere su COLORE per cambiare il colore di sfondo del carattere.\n\n" +
                        "I tasti dello ZOOM servono per cambiare la dimensione del carattere.\n"
                        );


                GradientDrawable gradient= (GradientDrawable) anteprima.getBackground();
                anteprima.setText("");
                tagAnteprima= (Map<String, Integer>) anteprima.getTag(); //ricavo il tag
                gradient.setColor(tagAnteprima.get("color"));   //setto il colore di anteprima
                gradient.setStroke(3, Color.BLACK);  //setto il bordo
                focusFlag=false;
                dialog.show();
                if (nonMostrareFlag)
                    dialogGuide.show();
            }
        });

        //se viene premuto buttonColor si apre la finestra di selezione colore
        buttonColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorPickerDialog.show(getFragmentManager(), null);

            }
        });

        //quando seleziono un colore aggiorno il colore di background del carattere di anteprima
        colorPickerDialog.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                GradientDrawable gradient= (GradientDrawable) anteprima.getBackground();
                gradient.setColor(color);
                tagAnteprima= (Map<String, Integer>) anteprima.getTag();
                tagAnteprima.put("color", color);
            }
        });

        //quando seleziono un colore per lo sfondo
        colorPickerDialogSfondo.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                coloreSfondo= color;
                primoLivello.setBackgroundColor(color);

                //Salvo il colore di sfondo nelle SharedPreferences
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("BackgroundColor", coloreSfondo);
                editor.apply();
            }
        });

        //Preparo dialog di salvataggio
        final Dialog dialogSave = new Dialog(this);
        dialogSave.setContentView(R.layout.dialog_save);
        dialogSave.setTitle("Salva configurazione");

        //gestisco i bottoni della dialog
        Button buttonSave= (Button) dialogSave.findViewById(R.id.buttonSave);
        Button buttonCancel= (Button) dialogSave.findViewById(R.id.buttonCancel);
        final EditText nameConfiguration= (EditText) dialogSave.findViewById(R.id.nameConfiguration);

        //TODO Se viene premuto il pulsante salva
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //controllo permessi di lettura e scrittura
                int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (result == PackageManager.PERMISSION_GRANTED){
                    //permesso approvato: SALVA
                    Boolean flagExistence;
                    File dir = Environment.getExternalStorageDirectory();
                    String path = dir.getAbsolutePath();
                    String fileName = nameConfiguration.getText().toString();
                    if(fileName.equals("")){
                        Toast.makeText(MainActivity.this,"Inserire un nome per la configurazione!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    //controllo esistenza cartella
                    File directory = new File(path +  File.separator + getString(R.string.app_name) + File.separator +"Configurazioni");
                    if(!directory.exists())
                        directory.mkdirs();

                    final File outputFile = new File(path + File.separator +getString(R.string.app_name)+
                            File.separator +"Configurazioni" +File.separator+fileName+".txt");
                    if(outputFile.exists())
                        flagExistence=true;
                    else
                        flagExistence=false;

                    if(flagExistence){  //SE il file esiste
                        //chiedo all'utente se sovrascrivere il file Creando una dialog
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                        alertDialogBuilder.setTitle("Attenzione");
                        alertDialogBuilder
                                .setMessage("Esiste già una configurazione con quel nome, vuoi sovrascriverla?");
                        // se l'utente decide di sovrascrivere
                        alertDialogBuilder.setPositiveButton("Si",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        //Salva configurazione
                                        saveConfigurationExternal(outputFile);
                                        dialogSave.dismiss();
                                        Toast.makeText(MainActivity.this,"Configurazione salvata!", Toast.LENGTH_LONG).show();
                                        dialog.cancel();
                                    }
                                });
                        // se l'utende decide di non sovrascrivere la configurazione
                        alertDialogBuilder.setNegativeButton("No",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        // show alert
                        alertDialog.show();
                    }
                    else{//SE il file NON esiste
                        //salvo la configurazione
                        saveConfigurationExternal(outputFile);
                        Toast.makeText(MainActivity.this,"Configurazione salvata!", Toast.LENGTH_LONG).show();
                        dialogSave.dismiss();
                        //attivo voce di ripristina
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("nessunaConfigurazione", false);
                        editor.apply();

                    }


                } else {
                    //RICHIESTA PERMESSO, RICHIEDO IL PERMESSO
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){

                        Toast.makeText(getApplicationContext(),"Per poter salvare la configurazione mi serve il permesso per accedere alla memoria esterna!",Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

                    } else {

                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    }

                }

            }
        });

        //Se viene premuto il pulsante annulla
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSave.dismiss();
            }
        });


        //tasto setting che apre un popUp menu
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(MainActivity.this, setting);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());

                //if nessuna configurazione salvata
                Boolean nessunaConfigurazione = sharedPref.getBoolean("nessunaConfigurazione", true);
                if(nessunaConfigurazione == true)
                    popup.getMenu().getItem(2).setEnabled(false);

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.attiva_guida:
                                checkBox.setChecked(false);
                                nonMostrareFlag = true;
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putBoolean("nonMostrareFlag", nonMostrareFlag);
                                editor.apply();

                                testo.setText("\nBenvenuto!\n\n" +
                                        "Premi il tasto + per inserire una lettera.\n\n" +
                                        "Premi il tasto in alto a destra per aprire il menù nel quale puoi:\n" +
                                        "-Cambiare il colore dello sfondo;\n" +
                                        "-Salvare e ripristinare ed eliminare la tua configurazione;\n" +
                                        "-Cancellare tutto" +
                                        "-Attivare la guida.\n");
                                dialogGuide.show();
                                break;
                            case R.id.colore_sfondo:
                                colorPickerDialogSfondo.show(getFragmentManager(), null);
                                break;
                            case R.id.salva_configurazione:

                                //Richiedo nome file
                                dialogSave.show();
                                break;
                            case R.id.ripristina_configurazione: //todo ripristina configurazione
                                //controllo permessi di lettura
                                int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);


                                if (result == PackageManager.PERMISSION_GRANTED){ //se il permesso è accordato
                                    //controllo esistenza cartella
                                    File dir = Environment.getExternalStorageDirectory();
                                    String path = dir.getAbsolutePath();
                                    File directory = new File(path + File.separator + getString(R.string.app_name));
                                    if(!directory.exists())
                                        directory.mkdirs();

                                    //lancio il fileChooser e decido coa fare con il file
                                    new FileChooser(MainActivity.this, getString(R.string.app_name),true, sharedPref).setFileListener(new FileChooser.FileSelectedListener() {
                                        @Override
                                        public void fileSelected(File file) {
                                            FileInputStream fis = null;
                                            FileOutputStream fos = null;
                                            try {
                                                //prendo la stringa json dal file selezionato
                                                fis = new FileInputStream(file);
                                                String sBuffer = "";
                                                InputStreamReader inputStreamReader= new InputStreamReader(fis);

                                                char[] inputBuffer = new char[1];
                                                while ( inputStreamReader.read(inputBuffer) != -1)
                                                {
                                                    sBuffer = sBuffer + new String(inputBuffer);
                                                }

                                                inputStreamReader.close();
                                                fis.close();


                                                //scrivo la stringa sul file setup.json
                                                fos = openFileOutput("setup.json", Context.MODE_PRIVATE);
                                                fos.write(sBuffer.getBytes());
                                                fos.close();

                                                //Rimango sulla pagina di configurazione
                                                SharedPreferences.Editor editor = sharedPref.edit();
                                                editor.putBoolean("noStart", true);
                                                editor.apply();

                                                //riavvio l'activity
                                                onStopAndLoadConfiguration();

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }).showDialog();

                                    testo.setText("\nSeleziona il file per aprirlo.\n\n" +
                                            "Tieni premuto su un file per eliminarlo.\n");
                                    if(nonMostrareFlag)
                                        dialogGuide.show();

                                }
                                else{  //se il permesso non è stato concesso
                                    //RICHIESTA PERMESSO, RICHIEDO IL PERMESSO
                                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){

                                        Toast.makeText(getApplicationContext(),"Per poter ripristinare la configurazione mi serve il permesso per accedere alla memoria esterna!",Toast.LENGTH_SHORT).show();
                                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                                    } else {

                                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                                    }
                                }

                                break;
                            case R.id.pulisci:
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setTitle("Attenzione");
                                builder.setMessage("Sei sicuro di voler cancellare tutto?");
                                builder.setCancelable(false);
                                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });

                                builder.setPositiveButton("SI", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FileOutputStream fos = null;
                                        String sBuffer="";
                                        try {
                                            fos = openFileOutput("setup.json", Context.MODE_PRIVATE);
                                            fos.write(sBuffer.getBytes());
                                            fos.close();
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        //Rimango sulla pagina di configurazione
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putBoolean("noStart", true);
                                        editor.apply();

                                        //riavvio l'activity
                                        onStopAndLoadConfiguration();
                                    }
                                });

                                AlertDialog alert = builder.create();
                                alert.show();
                                break;
                            case R.id.attiva_presentazione:
                                editor = sharedPref.edit();
                                editor.putBoolean("noPresentation", false);
                                editor.apply();

                                Intent mIntent = new Intent(MainActivity.this, welcome.class);
                                startActivity(mIntent);
                                finish();
                        }

                        return true;
                    }
                });

                popup.show();//showing popup menu
            }
        }
        );


        //pressione del tasto conferma della dialog
        conferma.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("prova",anteprima.getText().toString());
                if (anteprima.getText().toString().equals("\u0020"))
                    anteprima.setText("_");

                if(anteprima.getText().toString().equals("") == false) {
                    if (!focusFlag) {  // se sto aggiungendo un carattere
                        //aggiungo alla lista di caratteri
                        itemView = inflater.inflate(R.layout.item, l, false);
                        itemView.setOnLongClickListener(new LongPressListener(deleteIcon));  //Associo un longPressListener
                        itemView.setFocusableInTouchMode(true);
                        final TextView newCh = (TextView) itemView.findViewById(R.id.character); //carattere che andrà nel box


                        itemView.setOnFocusChangeListener(onFocusChangeListenerCharacter);  //gestisco la comparsa e scomparsa del tasto di modifica
                        tagAnteprima = (Map<String, Integer>) anteprima.getTag();
                        int color = tagAnteprima.get("color"); //ricavo il colore dall'anteprima

                        tagAnteprima.put("id", count);   //Associo un id che mi servirà per la ricerca

                        copyTextView(anteprima, newCh);   //Copio le caratteristiche dell'anteprima nel carattere newCh

                         //Setto posizione carattere TODO 21-07-2017
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) newCh.getLayoutParams();
                        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(newCh.getLayoutParams());
                        int top = layoutParams.topMargin + offset_t;
                        int left = layoutParams.leftMargin + offset_l;
                        offset_l = (offset_l + 48) %1600; //aumento l'offset ad ogni aggiunta
                        offset_t = (offset_t + 38) % 400; //aumento l'offset ad ogni aggiunta
                        marginParams.setMargins(left, top, 0, 0);
                        layoutParams = new RelativeLayout.LayoutParams(marginParams);
                        newCh.setLayoutParams(layoutParams);

                        //creo un nuovo oggetto Character e lo aggiungo alla lista
                        com.acqui.costa.alboboard.Character character = new com.acqui.costa.alboboard.Character(left, top, color,
                                newCh.getText().toString(), newCh.getTextSize(), count);
                        characters.add(character);  //aggiungo il carattere al mio array di caratteri

                        l.addView(itemView);
                        count++;
                    } else{  //se sto modificando un carattere
                        TextView chView = (TextView) l.getFocusedChild();
                        //cerco il carattere nel mio array di caratteri
                        for (com.acqui.costa.alboboard.Character ch : characters) {
                            tagAnteprima = (Map<String, Integer>) anteprima.getTag();
                            if (ch.getId() == tagAnteprima.get("id")) {
                                copyTextView(anteprima, chView);        //modifico il carattere graficamente(nel box)
                                GradientDrawable gradientDrawable = (GradientDrawable) chView.getBackground();
                                gradientDrawable.setStroke(7, Color.BLACK);
                                ch.copyCharacterFromTextView(anteprima);  //modifico il carattere nell'array
                            }
                        }
                    }
                    dialog.dismiss();  //chiudo la dialog

                    testo.setText("\nPer modificare il carattere inserito premi su di esso e poi sul tasto di modifica\n\n" +
                            "Tieni premuto sul carattere per spostarlo o eliminarlo.\n\n" +
                            "Quando hai terminato la configurazione premi sul tasto segno di spunta.\n");
                    if (nonMostrareFlag)
                        dialogGuide.show();
                }
                else{
                    Toast.makeText(MainActivity.this,"Devi inserire un carattere", Toast.LENGTH_SHORT).show();
                }

            }
        });


        //Gestisco lo zoom
        zoomControls.setZoomSpeed((long) 0.2);  //setto la velocità di ingrandimento

        //Zoom in
        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float size = anteprima.getTextSize();
                float product = (float) (size * 1.2);
                anteprima.setTextSize(TypedValue.COMPLEX_UNIT_PX, product);
            }
        });
        //Zoom out
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float size = anteprima.getTextSize();
                float product = (float) (size * 0.8);
                anteprima.setTextSize(TypedValue.COMPLEX_UNIT_PX, product);
            }
        });




        if (modify != null) {
            modify.setOnClickListener(modifyListener);
        }

    }


    //Pulsante di modifica
    View.OnClickListener modifyListener= new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView ch= (TextView) l.getFocusedChild();
            copyTextView(ch, anteprima);  //copio le caratteristche del character nell'anteprima
            focusFlag=true;
            dialog.show();
        }
    };




    //gestisco il focus per i tasti nel box
    View.OnFocusChangeListener onFocusChangeListenerCharacter= new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            GradientDrawable gradientDrawable= (GradientDrawable) v.getBackground();
            if (hasFocus) {
                assert modify != null;

                gradientDrawable.setStroke(7, Color.RED);  //setto il bordo
                gradientDrawable.setShape(GradientDrawable.RECTANGLE);
                gradientDrawable.setCornerRadius(10.f);
                modify.setVisibility(View.VISIBLE);  //rendo visibile il tasto modify
            }
            else {
                assert modify != null;
                gradientDrawable.setStroke(3, Color.BLACK);   //setto il bordo a 0
                modify.setVisibility(View.INVISIBLE);
            }
        }
    };

    //metodo che copia testo, colore, textSize e tag, da una textView ad un'altra
    public void copyTextView(TextView chS, TextView chD){
        chD.setText(chS.getText());  //setto il testo

        Map<String, Integer> tagS= (Map<String, Integer>) chS.getTag(); //ricavo il tag di destinazio

        //setto il colore e la forma del nuovo character
        GradientDrawable gradientDrawableD =new GradientDrawable();
        gradientDrawableD.setShape(GradientDrawable.RECTANGLE);
        gradientDrawableD.setCornerRadius(10.f);
        gradientDrawableD.setColor(tagS.get("color"));
        gradientDrawableD.setStroke(3, Color.BLACK);
        chD.setBackgroundDrawable(gradientDrawableD);


        Map<String, Integer> tagD=new HashMap<>();
        tagD.put("color",  tagS.get("color"));
        tagD.put("id", tagS.get("id"));

        chD.setTextSize(TypedValue.COMPLEX_UNIT_PX, chS.getTextSize()); //setto la dimesione del testo
        chD.setTag(tagD);  //setto il tag
    }

    /*
     *  TODO Metodo che permette il salvataggio della configurazione
     *
    */

    public  void saveConfiguration(String name){
        JSONArray array= new JSONArray();

        for (com.acqui.costa.alboboard.Character c: characters){
            JSONObject jsonObject= new JSONObject();
            c.characterToJson(jsonObject);
            array.put(jsonObject);
        }
        FileOutputStream fos ;
        try {
            fos = openFileOutput(name, Context.MODE_PRIVATE);
            fos.write(array.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     *  TODO Metodo che permette il salvataggio della configurazione su external storage
     *
    */

    public  void saveConfigurationExternal(File outputFile){
        JSONArray array= new JSONArray();

        for (com.acqui.costa.alboboard.Character c: characters){
            JSONObject jsonObject= new JSONObject();
            c.characterToJson(jsonObject);
            array.put(jsonObject);
        }

        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(array.toString().getBytes());
            fos.close();


        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    /*
     * Metodo che legge da un file una stringa json contentente la vecchia configurazione
     * da json ripristina tutti gli oggetti character
     *
    */

    public void lastSetupToCharacters(){
        try {
            FileInputStream fis = openFileInput("setup.json");
            String sBuffer = "";
            InputStreamReader inputStreamReader= new InputStreamReader(fis);

            char[] inputBuffer = new char[1];
            while ( inputStreamReader.read(inputBuffer) != -1)
            {
                sBuffer = sBuffer + new String(inputBuffer);
            }

            inputStreamReader.close();
            fis.close();

            characters.clear();
            JSONArray jsonArray= new JSONArray(sBuffer);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);


                int backgroundColor = object.getInt("backgroundColor");
                int left = object.getInt("left");
                int top= object.getInt("top");
                String character= object.getString("character");
                float textSize= (float) object.getDouble("textSize");
                int tag= object.getInt("tag");

                characters.add(new com.acqui.costa.alboboard.Character(left, top, backgroundColor, character, textSize, tag));

            }


        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

    }

    /*
    Gestione del tasto back
     */
    @Override
    public void onBackPressed() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Avviso");
            builder.setMessage("Sei sicuro di voler chiudere l'applicazione?");
            builder.setCancelable(false);
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.setPositiveButton("SI", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        }
        else
            Toast.makeText(MainActivity.this, "Premi ancora una volta per chiudere l'applicazione! ", Toast.LENGTH_SHORT).show();
        mLastClickTime = SystemClock.elapsedRealtime();

    }


    /*
    Metodo invocato prima della chiusura dell'activity corrente
    Salva la configurazione sotto  forma di stringa json
     */
    @Override
    protected void onStop() {
        super.onStop();
        JSONArray array= new JSONArray();
        saveConfiguration("setup.json");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(MainActivity.this,"Adesso puoi salvare la tua configurazione!", Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(MainActivity.this,"\"Permesso Negato, non è possibile salvare la configurazione senza il permesso!\"", Toast.LENGTH_SHORT).show();


                }
                break;
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(MainActivity.this,"Adesso puoi ripristinare la tua configurazione!", Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(MainActivity.this,"\"Permesso Negato, non è possibile ripristanare la configurazione senza il permesso!\"", Toast.LENGTH_SHORT).show();


                }
                break;
        }
    }

    /*
   Metodo invocato prima della chiusura dell'activity corrente
   Salva la configurazione sotto  forma di stringa json
    */
    protected void onStopAndLoadConfiguration() {
        Intent intent=getIntent();
        finish();
        startActivity(intent);

    }

}

