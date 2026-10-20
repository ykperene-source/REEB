package ci.billets.scanner;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.io.InputStream;
import java.util.Locale;

/**
 * Billet Scanner CI — prototype Android.
 * Estimation de surface basée sur le contour manuel d'un billet restant.
 * Ce prototype ne certifie ni l'authenticité ni le remboursement du billet.
 */
public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001, TAKE_PHOTO = 1002;
    private LinearLayout root, resultPanel;
    private ImageView imageView;
    private Bitmap bitmap;
    private OverlayView overlay;
    private Spinner denominationSpinner;
    private TextView percentText, missingText, statusText;
    private float[] points = new float[0];
    private Button analyzeButton, resetButton;
    private boolean photoMode = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, 1);
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(243,247,250));
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(24));
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18),dp(20),dp(18),dp(20));
        header.setBackground(shape(Color.rgb(16,42,67),20,0));
        ImageView logoView = new ImageView(this);
        logoView.setImageResource(R.drawable.reeb_logo);
        logoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(92), dp(92));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        header.addView(logoView, logoParams);
        addGap(header, 8);
        TextView brand = text("REEB",26,Color.WHITE,true); brand.setGravity(Gravity.CENTER);
        TextView tagline = text("Analyse • Calcule • Gère",14,Color.rgb(205,226,240),false); tagline.setGravity(Gravity.CENTER);
        header.addView(brand); addGap(header,6); header.addView(tagline); root.addView(header);
        addGap(root,16);

        TextView intro = text("1. Choisis ou photographie le billet",18,Color.rgb(16,42,67),true);
        root.addView(intro); addGap(root,10);

        LinearLayout buttons = new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button camera = button("📷  Photographier", true);
        Button gallery = button("🖼  Galerie", false);
        buttons.addView(camera,new LinearLayout.LayoutParams(0,dp(50),1));
        View spacer = new View(this); buttons.addView(spacer,new LinearLayout.LayoutParams(dp(10),1));
        buttons.addView(gallery,new LinearLayout.LayoutParams(0,dp(50),1)); root.addView(buttons);
        camera.setOnClickListener(v -> { photoMode=true; try { Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE); startActivityForResult(i,TAKE_PHOTO); } catch(Exception e){ toast("Appareil photo indisponible."); }});
        gallery.setOnClickListener(v -> { photoMode=false; Intent i=new Intent(Intent.ACTION_GET_CONTENT); i.setType("image/*"); startActivityForResult(i,PICK_IMAGE); });
        addGap(root,12);

        TextView hint = text("Conseil : place le billet sur un fond uni, bien éclairé, sans ombre. Pour une mesure plus fiable, photographie-le à plat.",13,Color.rgb(82,100,119),false);
        root.addView(hint); addGap(root,12);

        FrameLayout imageFrame = new FrameLayout(this);
        imageFrame.setBackground(shape(Color.WHITE,16,Color.rgb(217,226,236)));
        imageView = new ImageView(this); imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageFrame.addView(imageView,new FrameLayout.LayoutParams(-1,dp(240)));
        overlay = new OverlayView();
        imageFrame.addView(overlay,new FrameLayout.LayoutParams(-1,dp(240)));
        root.addView(imageFrame); addGap(root,8);

        statusText = text("Aucune image sélectionnée.",13,Color.rgb(82,100,119),false);
        root.addView(statusText); addGap(root,16);

        TextView step2 = text("2. Indique la valeur du billet",18,Color.rgb(16,42,67),true);
        root.addView(step2); addGap(root,8);
        denominationSpinner = new Spinner(this);
        String[] values = {"500 FCFA","1 000 FCFA","2 000 FCFA","5 000 FCFA","10 000 FCFA","Autre valeur"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);
        denominationSpinner.setAdapter(adapter);
        root.addView(denominationSpinner,new LinearLayout.LayoutParams(-1,dp(48)));
        addGap(root,16);

        TextView step3 = text("3. Délimite la partie restante",18,Color.rgb(16,42,67),true);
        root.addView(step3); addGap(root,6);
        root.addView(text("Après avoir chargé l'image, touche les 4 coins du billet restant dans l'ordre : haut-gauche, haut-droit, bas-droit, bas-gauche. Ajuste en recommençant si besoin.",13,Color.rgb(82,100,119),false));
        addGap(root,12);

        analyzeButton = button("CALCULER LE POURCENTAGE",true);
        root.addView(analyzeButton,new LinearLayout.LayoutParams(-1,dp(52)));
        analyzeButton.setOnClickListener(v -> analyze());
        addGap(root,8);
        resetButton = button("Recommencer le contour",false);
        root.addView(resetButton,new LinearLayout.LayoutParams(-1,dp(46)));
        resetButton.setOnClickListener(v -> { points=new float[0]; overlay.invalidate(); statusText.setText("Contour effacé. Touche les 4 coins du billet restant."); clearResult(); });
        addGap(root,16);

        resultPanel = new LinearLayout(this); resultPanel.setOrientation(LinearLayout.VERTICAL);
        resultPanel.setPadding(dp(18),dp(18),dp(18),dp(18)); resultPanel.setBackground(shape(Color.WHITE,18,Color.rgb(217,226,236)));
        resultPanel.addView(text("RÉSULTAT ESTIMATIF",13,Color.rgb(82,100,119),true));
        percentText = text("Surface restante : — %",24,Color.rgb(14,116,144),true); addGap(resultPanel,6); resultPanel.addView(percentText);
        missingText = text("Surface manquante : — %",17,Color.rgb(16,42,67),true); addGap(resultPanel,6); resultPanel.addView(missingText);
        resultPanel.addView(text("Cette valeur est une simple proportion mathématique, pas une promesse de remboursement.",12,Color.rgb(140,70,30),false));
        root.addView(resultPanel); addGap(root,14);
        TextView disclaimer = text("IMPORTANT : estimation indicative seulement. Le résultat dépend de la photo et du contour tracé. L'acceptation d'un billet endommagé dépend des règles de l'autorité monétaire et de l'examen du billet. L'application ne vérifie ni l'authenticité ni les critères officiels.",12,Color.rgb(82,100,119),false);
        disclaimer.setPadding(dp(4),dp(4),dp(4),dp(4)); root.addView(disclaimer);
    }

    private void clearResult() { percentText.setText("Surface restante : — %"); missingText.setText("Surface manquante : — %"); }
    private Button button(String label, boolean primary) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTypeface(null,1);
        b.setTextColor(primary?Color.WHITE:Color.rgb(16,42,67));
        b.setBackground(shape(primary?Color.rgb(14,116,144):Color.WHITE,14,primary?0:Color.rgb(190,205,218)));
        return b;
    }
    private void addGap(LinearLayout box,int height) { View v=new View(this); box.addView(v,new LinearLayout.LayoutParams(1,dp(height))); }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK) return;
        try {
            if(requestCode==PICK_IMAGE && data!=null) {
                Uri uri=data.getData(); InputStream in=getContentResolver().openInputStream(uri);
                bitmap=BitmapFactory.decodeStream(in); if(in!=null) in.close();
            } else if(requestCode==TAKE_PHOTO && data!=null && data.getExtras()!=null) {
                Object obj=data.getExtras().get("data"); if(obj instanceof Bitmap) bitmap=(Bitmap)obj;
            }
            if(bitmap!=null) {
                imageView.setImageBitmap(bitmap); points=new float[0]; overlay.invalidate();
                statusText.setText("Image chargée. Touche les 4 coins du billet restant dans l'image.");
                clearResult();
            }
        } catch(Exception e) { toast("Impossible d'ouvrir cette image."); }
    }

    private void analyze() {
        if(bitmap==null) { toast("Photographie ou sélectionne d'abord une image."); return; }
        if(points.length<8) { toast("Touche les 4 coins du billet restant dans l'image."); return; }
        double area=0;
        for(int i=0;i<4;i++) { int j=(i+1)%4; area += points[2*i]*points[2*j+1]-points[2*j]*points[2*i+1]; }
        area=Math.abs(area)/2.0;
        double fullArea=(double)overlay.getWidth()*overlay.getHeight();
        double pct=Math.max(0,Math.min(100,area/Math.max(1,fullArea)*100));
        // Because points are normalized to overlay coordinates, this is an estimate of the image area.
        percentText.setText(String.format(Locale.FRANCE,"Surface restante : %.1f %%",pct));
        missingText.setText(String.format(Locale.FRANCE,"Surface manquante : %.1f %%",100.0-pct));
        statusText.setText("Contour calculé. Vérifie que les 4 points entourent bien la partie restante.");
    }

    private class OverlayView extends View {
        Paint paint=new Paint(3);
        OverlayView() { super(MainActivity.this); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            if(points.length>=2) {
                paint.setColor(Color.rgb(14,116,144)); paint.setStrokeWidth(dp(3)); paint.setStyle(Paint.Style.STROKE);
                for(int i=0;i<points.length/2;i++) {
                    int j=(i+1)%(points.length/2);
                    c.drawLine(points[2*i]*getWidth(),points[2*i+1]*getHeight(),points[2*j]*getWidth(),points[2*j+1]*getHeight(),paint);
                }
                paint.setStyle(Paint.Style.FILL);
                for(int i=0;i<points.length/2;i++) c.drawCircle(points[2*i]*getWidth(),points[2*i+1]*getHeight(),dp(5),paint);
            }
            if(points.length<8) {
                paint.setColor(Color.rgb(14,116,144)); paint.setTextSize(dp(13)); paint.setStyle(Paint.Style.FILL);
                c.drawText("Points : "+(points.length/2)+"/4",dp(12),dp(22),paint);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if(e.getAction()!=MotionEvent.ACTION_DOWN || bitmap==null) return true;
            if(points.length>=8) { toast("Contour complet. Appuie sur « Recommencer le contour » pour recommencer."); return true; }
            float x=Math.max(0,Math.min(1,e.getX()/Math.max(1,getWidth())));
            float y=Math.max(0,Math.min(1,e.getY()/Math.max(1,getHeight())));
            float[] n=new float[points.length+2]; System.arraycopy(points,0,n,0,points.length); n[points.length]=x; n[points.length+1]=y; points=n;
            invalidate(); statusText.setText("Point "+(points.length/2)+"/4 ajouté.");
            return true;
        }
    }
}
