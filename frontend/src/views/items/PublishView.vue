<script setup>
import { getMyMemberships } from '@/api/modules/community';
import { createItem } from '@/api/modules/item';
import { useUserStore } from '@/store/user';
import { ITEM_CATEGORY } from '@/utils/constants';
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { onBeforeRouteLeave, useRouter } from 'vue-router';

const router = useRouter();
const user = useUserStore();
const submitting = ref(false);

// 提交时暂时阻止跳转
onBeforeRouteLeave(() => !submitting.value);

const form = reactive({
  title: '',
  category: '',
  communityId: '',
  dailyPrice: '',
  deposit: '',
  condition: '',
  description: '',
});

const error = ref('');
const message = ref('');
const joinedCommunities = ref([]);
const communityLoading = ref(true);
const communityError = ref('');

async function loadJoinedCommunities() {
  communityLoading.value = true;
  communityError.value = '';

  try {
    const memberships = await getMyMemberships();

    joinedCommunities.value = memberships.filter((membership) => membership.status === 'JOINED');

    const selectionExists = joinedCommunities.value.some(
      (community) => community.id === form.communityId,
    );

    if (!selectionExists) {
      form.communityId = joinedCommunities.value[0]?.id || '';
    }
  } catch (err) {
    joinedCommunities.value = [];
    form.communityId = '';
    communityError.value = err.message || '社区加载失败，请重试';
  } finally {
    communityLoading.value = false;
  }
}

onMounted(loadJoinedCommunities);

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

  if (communityLoading.value) {
    error.value = '社区正在加载，请稍后再试';
    return;
  }

  if (communityError.value) {
    error.value = '请先重新加载社区';
    return;
  }

  const hasJoinedCommunity = joinedCommunities.value.some(
    (community) => community.id === form.communityId,
  );

  if (!hasJoinedCommunity) {
    error.value = '请选择一个已加入的社区';
    return;
  }

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

  if (!user.isCertified) {
    error.value = '请先完成实名认证';
    return;
  }

  if (Number(form.deposit) > 500 && user.credit < 60) {
    error.value = '信用分不足，无法发布高押金物品';
    return;
  }

  return true;
}

function readImage(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error(`图片读取失败：${file.name}`));
    reader.onabort = () => reject(new Error('图片读取已取消'));

    reader.readAsDataURL(file);
  });
}

async function submitItem() {
  if (submitting.value || !checkForm()) return;

  submitting.value = true;

  // 在异步读取图片前保存本次提交的数据
  const payload = {
    title: form.title.trim(),
    category: form.category,
    communityId: form.communityId,
    dailyPrice: Number(form.dailyPrice),
    deposit: Number(form.deposit),
    condition: form.condition.trim(),
    description: form.description.trim(),
  };
  const files = images.value.map((image) => image.file);

  try {
    payload.images = await Promise.all(files.map(readImage));
    await createItem(payload);

    // 放行随后跳转到“我的物品”的导航
    submitting.value = false;
    await router.push('/my-items');
  } catch (err) {
    error.value = err.message || '提交失败，请重试';
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>发布物品</h1>
        <p>填写物品信息，审核通过后即可上架出租。</p>
      </div>
      <div class="actions">
        <router-link class="btn" to="/my-items"> 我的物品 </router-link>
        <router-link class="btn" to="/items"> 返回物品广场 </router-link>
      </div>
    </header>

    <div class="two">
      <div>
        <form
          class="panel"
          @submit.prevent="submitItem"
          @input="
            message = '';
            error = '';
          "
        >
          <fieldset class="publish-fields" :disabled="submitting">
            <div class="panel-title">
              <h2>物品信息</h2>
            </div>
            <label class="field">
              <span>所属社区 <em>*</em></span>
              <select
                v-model="form.communityId"
                :disabled="communityLoading || joinedCommunities.length === 0"
                required
              >
                <option value="" disabled>
                  {{ communityLoading ? '正在加载社区…' : '请选择已加入的社区' }}
                </option>
                <option
                  v-for="community in joinedCommunities"
                  :key="community.id"
                  :value="community.id"
                >
                  {{ community.name }}
                </option>
              </select>
            </label>

            <div v-if="communityError" role="alert">
              <p>{{ communityError }}</p>
              <button
                class="btn"
                type="button"
                :disabled="communityLoading"
                @click="loadJoinedCommunities"
              >
                重新加载社区
              </button>
            </div>

            <p v-else-if="!communityLoading && joinedCommunities.length === 0" class="help">
              请先前往
              <router-link to="/community">社区页面</router-link>
              加入一个社区，通过审核后即可发布物品。
            </p>
            <div class="form-grid">
              <label class="field">
                <span>物品标题 <em>*</em></span>
                <input
                  v-model="form.title"
                  type="text"
                  placeholder="例如：18V 充电式电钻"
                  required
                />
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
              <button class="btn primary" type="submit" :disabled="submitting || communityLoading">
                {{ submitting ? '正在提交…' : '提交审核' }}
              </button>
              <router-link class="btn" to="/items"> 取消 </router-link>
            </div>
          </fieldset>
        </form>
      </div>
    </div>
  </main>
</template>

<style scoped>
.publish-fields {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}
</style>
