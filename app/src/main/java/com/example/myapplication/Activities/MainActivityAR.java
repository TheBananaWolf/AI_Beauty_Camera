package com.example.myapplication.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.AugmentedFaceNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivityAR extends AppCompatActivity {
    private static Texture texture;
    private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeMap = new HashMap<>();
    Spinner s;
    Button Preview;
    private boolean isAdded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ar);

        String[] arraySpinner = new String[]{
                "Mask", "Mustache"
        };
        s = findViewById(R.id.function);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        Preview = findViewById(R.id.Preview);
        Preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = s.getSelectedItem().toString();
                if (text.equals("Mask")) {
                    if (isAdded) {
                        Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iterator = faceNodeMap.entrySet().iterator();
                        Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iterator.next();
                        AugmentedFace face = entry.getKey();
                        AugmentedFaceNode node = entry.getValue();
                        node.setParent(null);
                        iterator.remove();
                        isAdded = false;
                    }
                    Texture.builder()
                            .setSource(MainActivityAR.this, R.drawable.mustache1)
                            .build()
                            .thenAccept(textureModel -> MainActivityAR.texture = textureModel)
                            .exceptionally(throwable -> {
                                Toast.makeText(MainActivityAR.this, "cannot load texture", Toast.LENGTH_SHORT).show();
                                return null;
                            });
                    CustomArFragment customArFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
                    assert customArFragment != null;
                    customArFragment.getArSceneView().setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
                    customArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

                        Frame frame = customArFragment.getArSceneView().getArFrame();
                        assert frame != null;
                        Collection<AugmentedFace> augmentedFaces = frame.getUpdatedTrackables(AugmentedFace.class);

                        for (AugmentedFace augmentedFace : augmentedFaces) {
                            if (isAdded) return;

                            AugmentedFaceNode augmentedFaceMode = new AugmentedFaceNode(augmentedFace);
                            augmentedFaceMode.setParent(customArFragment.getArSceneView().getScene());
                            augmentedFaceMode.setFaceMeshTexture(texture);
                            faceNodeMap.put(augmentedFace, augmentedFaceMode);
                            isAdded = true;
                            Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iterator = faceNodeMap.entrySet().iterator();
                            Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iterator.next();
                            AugmentedFace face = entry.getKey();
                            while (face.getTrackingState() == TrackingState.STOPPED) {
                                AugmentedFaceNode node = entry.getValue();
                                node.setParent(null);
                                iterator.remove();
                            }

                        }
                    });
                }
                if (text.equals("Mustache")) {
                    if (isAdded) {
                        Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iterator = faceNodeMap.entrySet().iterator();
                        Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iterator.next();
                        AugmentedFace face = entry.getKey();

                        AugmentedFaceNode node = entry.getValue();
                        node.setParent(null);
                        iterator.remove();


                        isAdded = false;
                    }

                    Texture.builder()
                            .setSource(MainActivityAR.this, R.drawable.d)
                            .build()
                            .thenAccept(textureModel -> MainActivityAR.texture = textureModel)
                            .exceptionally(throwable -> {
                                Toast.makeText(MainActivityAR.this, "cannot load texture", Toast.LENGTH_SHORT).show();
                                return null;
                            });
                    CustomArFragment customArFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
                    assert customArFragment != null;
                    customArFragment.getArSceneView().setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
                    customArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

                        Frame frame = customArFragment.getArSceneView().getArFrame();
                        assert frame != null;
                        Collection<AugmentedFace> augmentedFaces = frame.getUpdatedTrackables(AugmentedFace.class);

                        for (AugmentedFace augmentedFace : augmentedFaces) {
                            if (isAdded) return;

                            AugmentedFaceNode augmentedFaceMode = new AugmentedFaceNode(augmentedFace);
                            augmentedFaceMode.setParent(customArFragment.getArSceneView().getScene());
                            augmentedFaceMode.setFaceMeshTexture(texture);
                            faceNodeMap.put(augmentedFace, augmentedFaceMode);
                            isAdded = true;
                            Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iterator = faceNodeMap.entrySet().iterator();
                            Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iterator.next();
                            AugmentedFace face = entry.getKey();
                            while (face.getTrackingState() == TrackingState.STOPPED) {
                                AugmentedFaceNode node = entry.getValue();
                                node.setParent(null);
                                iterator.remove();
                            }

                        }
                    });
                }
            }
        });


    }


}



