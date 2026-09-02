<template>
  <el-dialog
    v-model="dialogVisible"
    title="3D 模型预览"
    fullscreen
    :destroy-on-close="true"
    @close="handleClose"
  >
    <div class="viewer-container">
      <div class="viewer-toolbar">
        <el-button-group>
          <el-button
            :type="wireframe ? 'primary' : 'default'"
            size="small"
            @click="toggleWireframe"
          >
            {{ wireframe ? '实体模式' : '线框模式' }}
          </el-button>
          <el-button size="small" @click="resetCamera">重置视角</el-button>
          <el-button size="small" @click="toggleFullscreen">全屏</el-button>
        </el-button-group>
        <div class="model-info" v-if="modelInfo">
          <span>顶点: {{ modelInfo.vertices }}</span>
          <span>面数: {{ modelInfo.faces }}</span>
        </div>
      </div>

      <div class="viewer-canvas" ref="canvasRef">
        <div v-if="loading" class="viewer-loading">
          <el-icon class="is-loading" :size="40"><Loading /></el-icon>
          <p>加载模型中...</p>
        </div>
        <div v-if="loadError" class="viewer-error">
          <el-alert
            :title="loadError"
            type="error"
            show-icon
            :closable="false"
          />
          <p class="error-hint">3D 预览暂不可用，请下载文件后使用本地软件查看</p>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  modelPath: { type: String, default: '' }
})

const emit = defineEmits(['update:visible'])

const dialogVisible = ref(props.visible)
const canvasRef = ref(null)
const loading = ref(true)
const loadError = ref('')
const wireframe = ref(false)
const modelInfo = ref(null)

let scene = null
let camera = null
let renderer = null
let controls = null
let animationId = null
let currentModel = null

watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    loading.value = true
    loadError.value = ''
    nextTick(() => initScene())
  }
})

watch(dialogVisible, (val) => {
  emit('update:visible', val)
  if (!val) {
    disposeScene()
  }
})

function initScene() {
  if (!canvasRef.value) return

  const width = canvasRef.value.clientWidth
  const height = canvasRef.value.clientHeight

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xffffff)

  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
  camera.position.set(5, 5, 5)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.shadowMap.enabled = true
  canvasRef.value.appendChild(renderer.domElement)

  // 环境光
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)

  // 方向光
  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
  directionalLight.position.set(10, 20, 10)
  directionalLight.castShadow = true
  scene.add(directionalLight)

  // 网格地面
  const gridHelper = new THREE.GridHelper(10, 20, 0xcccccc, 0xe5e5e5)
  scene.add(gridHelper)

  // OrbitControls
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.mouseButtons = {
    LEFT: THREE.MOUSE.ROTATE,
    MIDDLE: THREE.MOUSE.PAN,
    RIGHT: THREE.MOUSE.PAN
  }

  window.addEventListener('resize', onResize)

  // 加载模型
  loadModel()

  animate()
}

function loadModel() {
  if (!props.modelPath) {
    loading.value = false
    loadError.value = '未提供模型路径'
    return
  }

  const loader = new GLTFLoader()
  loader.load(
    props.modelPath,
    (gltf) => {
      loading.value = false
      currentModel = gltf.scene

      // 计算模型信息
      let vertices = 0
      let faces = 0
      currentModel.traverse((child) => {
        if (child.isMesh) {
          vertices += child.geometry.attributes.position?.count || 0
          const index = child.geometry.index
          faces += index ? index.count / 3 : (child.geometry.attributes.position?.count || 0) / 3
        }
      })
      modelInfo.value = {
        vertices: Math.round(vertices),
        faces: Math.round(faces)
      }

      // 居中模型
      const box = new THREE.Box3().setFromObject(currentModel)
      const center = box.getCenter(new THREE.Vector3())
      const size = box.getSize(new THREE.Vector3())
      const maxDim = Math.max(size.x, size.y, size.z)
      const scale = 3 / maxDim
      currentModel.scale.setScalar(scale)
      currentModel.position.sub(center.multiplyScalar(scale))

      scene.add(currentModel)
    },
    (progress) => {
      // 加载进度
      const percent = Math.round((progress.loaded / progress.total) * 100)
      loading.value = true
    },
    (error) => {
      loading.value = false
      loadError.value = '模型加载失败，文件可能不支持3D预览'
      console.error('模型加载失败:', error)
    }
  )
}

function animate() {
  animationId = requestAnimationFrame(animate)
  if (controls) controls.update()
  if (renderer && scene && camera) {
    renderer.render(scene, camera)
  }
}

function onResize() {
  if (!canvasRef.value || !camera || !renderer) return
  const width = canvasRef.value.clientWidth
  const height = canvasRef.value.clientHeight
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

function toggleWireframe() {
  wireframe.value = !wireframe.value
  if (currentModel) {
    currentModel.traverse((child) => {
      if (child.isMesh) {
        child.material.wireframe = wireframe.value
      }
    })
  }
}

function resetCamera() {
  if (camera && controls) {
    camera.position.set(5, 5, 5)
    controls.target.set(0, 0, 0)
    controls.update()
  }
}

function toggleFullscreen() {
  if (canvasRef.value) {
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      canvasRef.value.requestFullscreen()
    }
  }
}

function disposeScene() {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  window.removeEventListener('resize', onResize)

  if (controls) {
    controls.dispose()
    controls = null
  }

  if (renderer) {
    renderer.dispose()
    if (renderer.domElement.parentNode) {
      renderer.domElement.parentNode.removeChild(renderer.domElement)
    }
    renderer = null
  }

  if (scene) {
    scene.traverse((child) => {
      if (child.geometry) child.geometry.dispose()
      if (child.material) {
        if (Array.isArray(child.material)) {
          child.material.forEach(m => m.dispose())
        } else {
          child.material.dispose()
        }
      }
    })
    scene.clear()
    scene = null
  }

  camera = null
  currentModel = null
  modelInfo.value = null
}

function handleClose() {
  dialogVisible.value = false
}

onUnmounted(() => {
  disposeScene()
})
</script>

<style scoped>
.viewer-container {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.viewer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 12px;
}

.model-info {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #606266;
}

.viewer-canvas {
  flex: 1;
  position: relative;
  border-radius: 4px;
  overflow: hidden;
  background: #f5f5f5;
}

.viewer-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 10;
}

.viewer-loading p {
  margin-top: 12px;
  font-size: 14px;
  color: #909399;
}

.viewer-error {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.error-hint {
  margin-top: 16px;
  font-size: 13px;
  color: #909399;
}
</style>
