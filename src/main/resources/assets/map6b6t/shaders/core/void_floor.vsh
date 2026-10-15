#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // Keep historical floor chunks visible beyond Minecraft's normal far plane.
    gl_Position.z = 0.0;
    vertexColor = Color;
}
