/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webank.mbank.ar.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.webank.mbank.ar.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;



/** Renders an object loaded from an OBJ file in OpenGL.
 * 渲染从3D模型中加载的物体
 * */
public class ObjectRenderer {
  private static final String TAG = ObjectRenderer.class.getSimpleName();

  /**
   * Blend mode.
   *
   * @see #setBlendMode(BlendMode)
   */
  public enum BlendMode {
    /** Multiplies the destination color by the source alpha. */
    Shadow,
    /** Normal alpha blending. */
    Grid
  }

  private static final int COORDS_PER_VERTEX = 3;

  // Note: the last component must be zero to avoid applying the translational part of the matrix.
  private static final float[] LIGHT_DIRECTION = new float[] {0.250f, 0.866f, 0.433f, 0.0f};
  private final float[] viewLightDirection = new float[4];

  // Object vertex buffer variables.
  private int vertexBufferId;
  private int verticesBaseAddress;
  private int texCoordsBaseAddress;
  private int normalsBaseAddress;
  private int indexBufferId;
  private int indexCount;

  private int program;
  private final int[] textures = new int[1];

  // Shader location: model view projection matrix.
  private int modelViewUniform;
  private int modelViewProjectionUniform;

  // Shader location: object attributes.
  private int positionAttribute;
  private int normalAttribute;
  private int texCoordAttribute;

  // Shader location: texture sampler.
  private int textureUniform;

  // Shader location: environment properties.
  private int lightingParametersUniform;

  // Shader location: material properties.
  private int materialParametersUniform;

  private BlendMode blendMode = null;

  // Temporary matrices allocated here to reduce number of allocations for each frame.
  private final float[] modelMatrix = new float[16];
  private final float[] modelViewMatrix = new float[16];
  private final float[] modelViewProjectionMatrix = new float[16];

  // Set some default material properties to use for lighting.
  private float ambient = 0.3f;
  private float diffuse = 1.0f;
  private float specular = 1.0f;
  private float specularPower = 6.0f;

  public ObjectRenderer() {}

  /**
   * Creates and initializes OpenGL resources needed for rendering the model.
   *创建并初始化OpenGL渲染的模型
   * @param context Context for loading the shader and below-named model and texture assets.
   * @param objAssetName Name of the OBJ file containing the model geometry.
   * @param diffuseTextureAssetName Name of the PNG file containing the diffuse texture map.
   */
  public void createOnGlThread(Context context, String objAssetName, String diffuseTextureAssetName)
      throws IOException {
    // Read the texture.
    Bitmap textureBitmap =
        BitmapFactory.decodeStream(context.getAssets().open(diffuseTextureAssetName));

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glGenTextures(textures.length, textures, 0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    textureBitmap.recycle();

    ShaderUtil.checkGLError(TAG, "Texture loading");

    // Read the obj file.
    InputStream objInputStream = context.getAssets().open(objAssetName);
    Obj obj = ObjReader.read(objInputStream);

    // Prepare the Obj so that its structure is suitable for
    // rendering with OpenGL:准备Obj，使其结构适合用OpenGL进行渲染：
    // 1. Triangulate it对其进行三角测量
    // 2. Make sure that texture coordinates are not ambiguous
    // 3. Make sure that normals（法线） are not ambiguous
    // 4. Convert it to single-indexed data将其转换为单索引数据
    obj = ObjUtils.convertToRenderable(obj);

    // OpenGL does not use Java arrays. ByteBuffers are used instead to provide data in a format
    // that OpenGL understands.

    // Obtain the data from the OBJ, as direct buffers:
    //返回obj面处的顶点索引，返回类型为IntBuffer
    IntBuffer wideIndices = ObjData.getFaceVertexIndices(obj, 3);
    FloatBuffer vertices = ObjData.getVertices(obj);
    FloatBuffer texCoords = ObjData.getTexCoords(obj, 2);
    FloatBuffer normals = ObjData.getNormals(obj);

    /*//debug start
    int[] verticesArray = ObjData.getFaceVertexIndicesArray(obj);
    for (int i = 0; i < verticesArray.length; i++) {
      System.out.println("VertexIndicesArray["+i+"]="+verticesArray[i]);
    }
    //debug end*/

    // Convert int indices to shorts for GL ES 2.0 compatibility
    ShortBuffer indices =
        ByteBuffer.allocateDirect(2 * wideIndices.limit())
            .order(ByteOrder.nativeOrder())
            .asShortBuffer();

    //检测wideIndices是否已经全部取完
    while (wideIndices.hasRemaining()) {
      indices.put((short) wideIndices.get());
    }
    //倒回缓冲区
    indices.rewind();

    int[] buffers = new int[2];
    //在CPU中创建两个buffer，用于存储数据并传递给GPU，提高效率
    GLES20.glGenBuffers(2, buffers, 0);
    vertexBufferId = buffers[0];
    indexBufferId = buffers[1];

    // Load vertex buffer
    //计算各种类型数据的大小，方便后续在内存中存放
    verticesBaseAddress = 0;
    texCoordsBaseAddress = verticesBaseAddress + 4 * vertices.limit();
    normalsBaseAddress = texCoordsBaseAddress + 4 * texCoords.limit();
    //计算中的数据大小，方便后续开闭内存空间
    final int totalBytes = normalsBaseAddress + 4 * normals.limit();

    //指定GPU使用的buffer：类型+buffer
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
    //在绑定的buffer中，开辟一块大小为totalBytes的内存
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW);
    //在上述开辟的内存中，先存放4 * vertices.limit()大小的顶点数据，然后存放4 * texCoords.limit()大小的纹理坐标数据
    //最后存放4 * normals.limit()大小的法线数据
    GLES20.glBufferSubData(
        GLES20.GL_ARRAY_BUFFER, verticesBaseAddress, 4 * vertices.limit(), vertices);
    GLES20.glBufferSubData(
        GLES20.GL_ARRAY_BUFFER, texCoordsBaseAddress, 4 * texCoords.limit(), texCoords);
    GLES20.glBufferSubData(
        GLES20.GL_ARRAY_BUFFER, normalsBaseAddress, 4 * normals.limit(), normals);
    //通过上述操作，已经成功把 CPU 端保存的数据传递给 GPU 端, 保存在指定的 buffer object 中。
    // 所以可以将vertexBufferId这个buffer与GPU解绑
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    //将indexBufferId这个buffer与GPU绑定
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
    indexCount = indices.limit();
    //在indexBuffer中开辟2 * indexCount的内存块，并存放indices数据
    GLES20.glBufferData(
        GLES20.GL_ELEMENT_ARRAY_BUFFER, 2 * indexCount, indices, GLES20.GL_STATIC_DRAW);
    //buffer与GPU解绑
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

    ShaderUtil.checkGLError(TAG, "OBJ buffer load");

    final int vertexShader =
        ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, R.raw.object_vertex);
    final int fragmentShader =
        ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, R.raw.object_fragment);

    program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vertexShader);
    GLES20.glAttachShader(program, fragmentShader);
    GLES20.glLinkProgram(program);
    GLES20.glUseProgram(program);

    ShaderUtil.checkGLError(TAG, "Program creation");

    modelViewUniform = GLES20.glGetUniformLocation(program, "u_ModelView");
    modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection");

    positionAttribute = GLES20.glGetAttribLocation(program, "a_Position");
    normalAttribute = GLES20.glGetAttribLocation(program, "a_Normal");
    texCoordAttribute = GLES20.glGetAttribLocation(program, "a_TexCoord");

    textureUniform = GLES20.glGetUniformLocation(program, "u_Texture");

    lightingParametersUniform = GLES20.glGetUniformLocation(program, "u_LightingParameters");
    materialParametersUniform = GLES20.glGetUniformLocation(program, "u_MaterialParameters");

    ShaderUtil.checkGLError(TAG, "Program parameters");

    //创建单位矩阵，并保存在modelMatrix中
    Matrix.setIdentityM(modelMatrix, 0);
  }

  /**
   * Selects the blending mode for rendering.
   *选择混合模式进行渲染。
   * @param blendMode The blending mode. Null indicates no blending (opaque rendering).
   */
  public void setBlendMode(BlendMode blendMode) {
    this.blendMode = blendMode;
  }

  /**
   * Updates the object model matrix and applies scaling.
   *
   * @param modelMatrix A 4x4 model-to-world transformation matrix, stored in column-major order.
   * @param scaleFactor A separate scaling factor to apply before the {@code modelMatrix}.
   * @see android.opengl.Matrix
   */
  public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
    float[] scaleMatrix = new float[16];
    Matrix.setIdentityM(scaleMatrix, 0);
    scaleMatrix[0] = scaleFactor;
    scaleMatrix[5] = scaleFactor;
    scaleMatrix[10] = scaleFactor;
    Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
  }

  /**
   * Sets the surface characteristics of the rendered model.
   *
   * @param ambient Intensity of non-directional surface illumination.非定向表面照明的强度
   * @param diffuse Diffuse (matte) surface reflectivity.漫反射（无光泽）表面反射率。
   * @param specular Specular (shiny) surface reflectivity.镜面（有光泽）表面反射率。
   * @param specularPower Surface shininess. Larger values result in a smaller, sharper specular
   *     highlight.表面光泽。 较大的值会导致更小，更清晰的镜面高光。
   */
  public void setMaterialProperties(
      float ambient, float diffuse, float specular, float specularPower) {
    this.ambient = ambient;
    this.diffuse = diffuse;
    this.specular = specular;
    this.specularPower = specularPower;
  }

  /**
   * Draws the model.
   *
   * @param cameraView A 4x4 view matrix, in column-major order.
   * @param cameraPerspective A 4x4 projection matrix, in column-major order.
   * @param lightIntensity Illumination intensity. Combined with diffuse and specular material
   *     properties.y照明强度。 结合漫反射和镜面材料属性。
   * @see #setBlendMode(BlendMode)
   * @see #updateModelMatrix(float[], float)
   * @see #setMaterialProperties(float, float, float, float)
   * @see android.opengl.Matrix
   */
  public void draw(float[] cameraView, float[] cameraPerspective, float lightIntensity) {

    ShaderUtil.checkGLError(TAG, "Before draw");

    // Build the ModelView and ModelViewProjection matrices
    // for calculating object position and light.

    Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
    Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

    GLES20.glUseProgram(program);

    // Set the lighting environment properties.

    Matrix.multiplyMV(viewLightDirection, 0, modelViewMatrix, 0, LIGHT_DIRECTION, 0);
    normalizeVec3(viewLightDirection);
    GLES20.glUniform4f(
        lightingParametersUniform,
        viewLightDirection[0],
        viewLightDirection[1],
        viewLightDirection[2],
        lightIntensity);


    // Set the object material properties.
    GLES20.glUniform4f(materialParametersUniform, ambient, diffuse, specular, specularPower);

    // Attach the object texture.
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
    GLES20.glUniform1i(textureUniform, 0);

    // Set the vertex attributes.
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);

    GLES20.glVertexAttribPointer(
        positionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, verticesBaseAddress);
    GLES20.glVertexAttribPointer(normalAttribute, 3, GLES20.GL_FLOAT, false, 0, normalsBaseAddress);
    GLES20.glVertexAttribPointer(
        texCoordAttribute, 2, GLES20.GL_FLOAT, false, 0, texCoordsBaseAddress);

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    // Set the ModelViewProjection matrix in the shader.
    GLES20.glUniformMatrix4fv(modelViewUniform, 1, false, modelViewMatrix, 0);
    GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

    // Enable vertex arrays
    GLES20.glEnableVertexAttribArray(positionAttribute);
    GLES20.glEnableVertexAttribArray(normalAttribute);
    GLES20.glEnableVertexAttribArray(texCoordAttribute);

//    if (blendMode != null) {
//      GLES20.glDepthMask(false);
//      GLES20.glEnable(GLES20.GL_BLEND);
//      switch (blendMode) {
//        case Shadow:
//          // Multiplicative blending function for Shadow.
//          GLES20.glBlendFunc(GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//          break;
//        case Grid:
//          // Grid, additive blending function.
//          GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//          break;
//      }
//    }

    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

//    if (blendMode != null) {
//      GLES20.glDisable(GLES20.GL_BLEND);
//      GLES20.glDepthMask(true);
//    }

    // Disable vertex arrays
    GLES20.glDisableVertexAttribArray(positionAttribute);
    GLES20.glDisableVertexAttribArray(normalAttribute);
    GLES20.glDisableVertexAttribArray(texCoordAttribute);

    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    ShaderUtil.checkGLError(TAG, "After draw");
  }

  private static void normalizeVec3(float[] v) {
    float reciprocalLength = 1.0f / (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    v[0] *= reciprocalLength;
    v[1] *= reciprocalLength;
    v[2] *= reciprocalLength;
  }

}
