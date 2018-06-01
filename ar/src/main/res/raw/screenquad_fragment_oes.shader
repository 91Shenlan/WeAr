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
  //值得注意的是，在Android中Camera产生的preview texture是以一种特殊的格式传送的，
  //因此shader里的纹理类型并不是普通的sampler2D,而是samplerExternalOES, 在shader的头部也必须声明OES 的扩展。
  //除此之外，external OES的纹理和Sampler2D在使用时没有差别。
#extension GL_OES_EGL_image_external : require

precision mediump float;
varying vec2 v_TexCoord;
uniform samplerExternalOES sTexture;


void main() {
    gl_FragColor = texture2D(sTexture, v_TexCoord);
}
