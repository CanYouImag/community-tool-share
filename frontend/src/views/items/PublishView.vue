<script setup>
import { ITEM_CATEGORY } from '@/utils/constants';
import { onBeforeUnmount, reactive, ref } from 'vue';

const form = reactive({
  title: '',
  category: '',
  dailyPrice: '',
  deposit: '',
  condition: '',
  description: '',
});

const error = ref('');
const message = ref('');
const images = ref([]);
const allowedImageTypes = ['image/jpeg', 'image/png', 'image/webp'];
const maxImageSize = 5 * 1024 * 1024;

function selectImages(event) {
  const files = Array.from(event.target.files || []);

  // 清空选择器，允许移除后再次选择同一张图片
  event.target.value = '';

  if (files.length === 0) return;

  error.value = '';
  message.value = '';

  const invalidFile = files.find(
    (file) => !allowedImageTypes.includes(file.type) || file.size === 0 || file.size > maxImageSize,
  );

  if (invalidFile) {
    error.value = `“${invalidFile.name}”不符合要求：请选择非空的 JPG、PNG 或 WebP 图片，单张不超过 5MB。`;
    return;
  }

  images.value.push(
    ...files.map((file) => ({
      file,
      url: URL.createObjectURL(file),
    })),
  );
}

function removeImage(index) {
  const [removed] = images.value.splice(index, 1);

  if (removed) {
    URL.revokeObjectURL(removed.url);
  }

  error.value = '';
  message.value = '';
}

onBeforeUnmount(() => {
  images.value.forEach((image) => {
    URL.revokeObjectURL(image.url);
  });
});

function checkForm() {
  error.value = '';
  message.value = '';

  if (!form.title.trim() || !form.condition.trim() || !form.description.trim()) {
    error.value = '标题、成色说明和物品描述不能为空或只有空格';
    return;
  }

  if (!ITEM_CATEGORY.includes(form.category)) {
    error.value = '请选择物品分类';
    return;
  }

  if (images.value.length === 0) {
    error.value = '请至少添加一张物品图片';
    return;
  }

  message.value = '物品信息和图片检查通过，尚未提交审核。';
}
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>发布物品</h1>
        <p>填写物品信息，审核通过后即可上架出租。</p>
      </div>
      <router-link class="btn" to="/items"> 返回物品广场 </router-link>
    </header>

    <div class="two">
      <div>
        <form
          class="panel"
          @submit.prevent="checkForm"
          @input="
            message = '';
            error = '';
          "
        >
          <div class="panel-title">
            <h2>物品信息</h2>
          </div>

          <div class="form-grid">
            <label class="field">
              <span>物品标题 <em>*</em></span>
              <input v-model="form.title" type="text" placeholder="例如：18V 充电式电钻" required />
            </label>

            <label class="field">
              <span>分类 <em>*</em></span>
              <select v-model="form.category" required>
                <option value="" disabled>请选择分类</option>
                <option v-for="name in ITEM_CATEGORY" :key="name" :value="name">
                  {{ name }}
                </option>
              </select>
            </label>

            <label class="field">
              <span>日租金（元）<em>*</em></span>
              <input
                v-model="form.dailyPrice"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="20.00"
                required
              />
            </label>

            <label class="field">
              <span>押金（元）<em>*</em></span>
              <input
                v-model="form.deposit"
                type="number"
                min="0"
                step="0.01"
                placeholder="200.00"
                required
              />
            </label>
          </div>

          <label class="field">
            <span>成色说明 <em>*</em></span>
            <input
              v-model="form.condition"
              type="text"
              placeholder="例如：九成新，功能正常"
              required
            />
          </label>

          <label class="field">
            <span>物品描述 <em>*</em></span>
            <textarea
              v-model="form.description"
              rows="5"
              placeholder="介绍用途、配件和使用注意事项"
              required
            ></textarea>
          </label>

          <label class="upload">
            <strong>＋ 添加物品图片</strong>
            <span>至少一张，支持 JPG、PNG、WebP，单张不超过 5MB</span>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              multiple
              aria-label="添加物品图片"
              @change="selectImages"
            />
          </label>

          <template v-if="images.length > 0">
            <p class="help">已选择 {{ images.length }} 张，第一张作为封面。</p>

            <div class="thumbs">
              <div v-for="(image, index) in images" :key="image.url">
                <img
                  class="art small"
                  :src="image.url"
                  :alt="`物品图片 ${index + 1}：${image.file.name}`"
                />
                <button
                  class="btn small"
                  type="button"
                  :aria-label="`移除第 ${index + 1} 张图片`"
                  @click="removeImage(index)"
                >
                  移除
                </button>
              </div>
            </div>
          </template>

          <p v-if="error" role="alert">{{ error }}</p>
          <p v-if="message" role="status">{{ message }}</p>

          <hr class="rule" />

          <div class="actions">
            <button class="btn primary" type="submit">检查表单</button>
            <router-link class="btn" to="/items"> 取消 </router-link>
          </div>
        </form>
      </div>
    </div>
  </main>
</template>
