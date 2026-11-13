<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router';

import { getMyMemberships } from '@/api/modules/community';
import { createItem, getCategories, getItem, updateItem, uploadItemImage } from '@/api/modules/item';
import { useUserStore } from '@/store/user';

const router = useRouter();
const route = useRoute();
const isEdit = computed(() => Boolean(route.params.id));
const editLoading = ref(false);
const editLoadError = ref('');
const user = useUserStore();
const submitting = ref(false);
const submissionComplete = ref(false);

const form = reactive({
  title: '',
  categoryId: '',
  communityId: '',
  rentPerDay: '',
  deposit: '',
  condition: '',
  description: '',
});

const error = ref('');
const message = ref('');
const joinedCommunities = ref([]);
const categories = ref([]);
const communityLoading = ref(true);
const communityError = ref('');

function clearFeedback() {
  message.value = '';
  error.value = '';
}

async function loadJoinedCommunities() {
  if (!user.isLoggedIn || !user.isCertified) {
    joinedCommunities.value = [];
    communityLoading.value = false;
    return;
  }

  communityLoading.value = true;
  communityError.value = '';

  try {
    const memberships = await getMyMemberships();

    joinedCommunities.value = memberships.filter((membership) => membership.status === 'JOINED');

    const selectionExists = joinedCommunities.value.some(
      (community) => community.id === form.communityId,
    );

    if (!selectionExists && !isEdit.value) {
      const previousCommunity = form.communityId;
      form.communityId = joinedCommunities.value[0]?.id || '';
      // 自动选择默认社区不算用户修改，其他已填写字段仍然保留。
      if (baseline.value?.fields.communityId === previousCommunity) {
        baseline.value.fields.communityId = form.communityId;
      }
    }
  } catch (err) {
    joinedCommunities.value = [];
    communityError.value = err.message || '社区加载失败，请重试';
  } finally {
    communityLoading.value = false;
  }
}

async function loadCategories() {
  try {
    categories.value = await getCategories();
  } catch {
    categories.value = [];
  }
}

onMounted(() => {
  loadJoinedCommunities();
  loadCategories();
});

const images = ref([]);
const baseline = ref(null);

function formSnapshot() {
  return {
    fields: { ...form, rentPerDay: String(form.rentPerDay), deposit: String(form.deposit) },
    images: images.value.map((image) => image.url),
  };
}

function markSaved() {
  baseline.value = formSnapshot();
}

const hasUnsavedChanges = computed(
  () =>
    !submissionComplete.value &&
    !editLoading.value &&
    !editLoadError.value &&
    baseline.value !== null &&
    JSON.stringify(formSnapshot()) !== JSON.stringify(baseline.value),
);

function confirmLeave() {
  if (submitting.value) return false;
  return (
    !hasUnsavedChanges.value || window.confirm('填写的内容尚未提交，离开后将丢失。确定离开吗？')
  );
}

onBeforeRouteLeave(confirmLeave);
onBeforeRouteUpdate((to, from) => {
  if (submitting.value) return false;
  return to.params.id === from.params.id || confirmLeave();
});

function confirmBeforeUnload(event) {
  if (!hasUnsavedChanges.value && !submitting.value) return;
  event.preventDefault();
  event.returnValue = '';
}

watch(
  () => hasUnsavedChanges.value || submitting.value,
  (needsConfirmation) => {
    if (needsConfirmation) window.addEventListener('beforeunload', confirmBeforeUnload);
    else window.removeEventListener('beforeunload', confirmBeforeUnload);
  },
  { flush: 'sync' },
);
onBeforeUnmount(() => window.removeEventListener('beforeunload', confirmBeforeUnload));

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
      key: crypto.randomUUID(),
      file,
      url: URL.createObjectURL(file),
    })),
  );
}

function removeImage(index) {
  const [removed] = images.value.splice(index, 1);

  if (removed?.file) {
    URL.revokeObjectURL(removed.url);
  }

  error.value = '';
  message.value = '';
}

function releaseImages() {
  images.value.forEach((image) => {
    if (image.file) {
      URL.revokeObjectURL(image.url);
    }
  });
  images.value = [];
}

onBeforeUnmount(releaseImages);

function checkForm() {
  error.value = '';
  message.value = '';

  if (!user.isLoggedIn) {
    error.value = '请先登录';
    return;
  }

  if (!user.isCertified) {
    error.value = '请先完成实名认证';
    return;
  }

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

  if (!form.categoryId) {
    error.value = '请选择物品分类';
    return;
  }

  if (images.value.length === 0) {
    error.value = '请至少添加一张物品图片';
    return;
  }

  if (Number(form.deposit) > 500 && user.credit < 60) {
    error.value = '信用分不足，无法发布高押金物品';
    return;
  }

  return true;
}

async function submitItem() {
  if (
    submitting.value ||
    submissionComplete.value ||
    editLoading.value ||
    editLoadError.value ||
    !checkForm()
  ) {
    return;
  }

  submitting.value = true;

  const editingId = isEdit.value ? String(route.params.id) : null;

  const payload = {
    title: form.title.trim(),
    categoryId: Number(form.categoryId),
    communityId: Number(form.communityId),
    rentPerDay: Number(form.rentPerDay),
    deposit: Number(form.deposit),
    condition: form.condition.trim(),
    description: form.description.trim(),
  };

  const pictureSnapshot = images.value.map((image) => ({
    file: image.file,
    url: image.url,
  }));

  try {
    // 新选择的文件上传到后端换取 URL，已保存的图片沿用原 URL
    payload.images = (
      await Promise.all(
        pictureSnapshot.map((image) =>
          image.file ? uploadItemImage(image.file).then((r) => r.url) : image.url,
        ),
      )
    ).filter((url) => typeof url === 'string' && url.trim());

    if (editingId) {
      await updateItem(editingId, payload);
    } else {
      await createItem(payload);
    }

    submissionComplete.value = true;
    markSaved();
    message.value = '提交成功，可在“我的物品”查看审核状态。';
    submitting.value = false;
    await router.push('/my-items');
  } catch (err) {
    error.value = submissionComplete.value
      ? '已提交成功，但页面跳转失败。请点击“我的物品”查看。'
      : err.message || '提交失败，请重试';
  } finally {
    submitting.value = false;
  }
}

watch(
  () => route.params.id,
  async (id, _oldId, onCleanup) => {
    let cancelled = false;
    onCleanup(() => {
      cancelled = true;
    });

    releaseImages();
    submissionComplete.value = false;
    error.value = '';
    message.value = '';
    editLoadError.value = '';
    editLoading.value = Boolean(id);

    Object.assign(form, {
      title: '',
      categoryId: '',
      communityId: joinedCommunities.value[0]?.id || '',
      rentPerDay: '',
      deposit: '',
      condition: '',
      description: '',
    });
    markSaved();

    if (!id || !user.isLoggedIn || !user.isCertified) {
      editLoading.value = false;
      return;
    }

    try {
      const item = await getItem(id);
      if (cancelled) return;

      Object.assign(form, {
        title: item.title,
        categoryId: String(item.categoryId ?? ''),
        communityId: item.communityId,
        rentPerDay: item.rentPerDay ?? '',
        deposit: item.deposit ?? '',
        condition: item.condition || '',
        description: item.description || '',
      });

      images.value = (item.images || []).map((url, index) => ({
        key: `saved-${index}`,
        file: null,
        url,
      }));
      markSaved();
    } catch (err) {
      if (!cancelled) {
        editLoadError.value = err.message || '物品加载失败';
      }
    } finally {
      if (!cancelled) {
        editLoading.value = false;
      }
    }
  },
  { immediate: true },
);
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <h1>{{ isEdit ? '编辑物品' : '发布物品' }}</h1>
      <div class="actions">
        <router-link class="btn" to="/my-items"> 我的物品 </router-link>
      </div>
    </header>

    <section v-if="!user.isLoggedIn" class="panel" role="alert">
      <h2>请先登录</h2>
      <p>登录并完成实名认证后，才能发布或编辑物品。</p>
    </section>

    <section v-else-if="!user.isCertified" class="panel" role="alert">
      <h2>请先完成实名认证</h2>
    </section>

    <section v-else-if="editLoading" class="panel" role="status">正在加载物品…</section>

    <section v-else-if="editLoadError" class="panel" role="alert">
      <p>{{ editLoadError }}</p>
      <router-link class="btn" to="/my-items"> 返回我的物品 </router-link>
    </section>

    <div v-else class="two">
      <div>
        <form class="panel" @submit.prevent="submitItem" @input="clearFeedback">
          <fieldset class="publish-fields" :disabled="submitting || submissionComplete">
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
                <select v-model="form.categoryId" required>
                  <option value="" disabled>请选择分类</option>
                  <option v-for="c in categories" :key="c.id" :value="String(c.id)">
                    {{ c.name }}
                  </option>
                </select>
              </label>

              <label class="field">
                <span>日租金（元）<em>*</em></span>
                <input
                  v-model="form.rentPerDay"
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
              <p class="help">{{ images.length }} 张图片，首张为封面</p>

              <div class="thumbs">
                <div v-for="(image, index) in images" :key="image.key">
                  <img
                    class="art small"
                    :src="image.url"
                    :alt="`物品图片 ${index + 1}：${image.file?.name || '已保存图片'}`"
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
              <button
                class="btn primary"
                type="submit"
                :disabled="submitting || submissionComplete || communityLoading"
              >
                {{
                  submissionComplete
                    ? '已提交审核'
                    : submitting
                      ? '正在提交…'
                      : isEdit
                        ? '重新提交审核'
                        : '提交审核'
                }}
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
