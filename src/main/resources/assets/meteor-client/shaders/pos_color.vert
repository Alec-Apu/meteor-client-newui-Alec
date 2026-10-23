#version 450 core

#ifdef METEOR_UI
layout (location = 0) in vec2 Position;
#else
layout (location = 0) in vec3 Position;
#endif
layout (location = 1) in vec4 Color;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

layout (location = 0) out vec4 v_Color;

void main() {
#ifdef METEOR_UI
    gl_Position = u_Proj * u_ModelView * vec4(Position, 0.0, 1.0);
#else
    gl_Position = u_Proj * u_ModelView * vec4(Position, 1.0);
#endif

    v_Color = Color;
}
