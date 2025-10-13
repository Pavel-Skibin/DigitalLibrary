<template>
  <div class="author-card" @click="$emit('select', author)">
    <div class="author-avatar">
      {{ getInitials(author.fullName) }}
    </div>
    <div class="author-info">
      <h3 class="author-name">{{ author.fullName }}</h3>
      <div class="author-meta">
        <span class="book-count" v-if="author.bookCount">
          📚 {{ author.bookCount }} {{ pluralizeBooks(author.bookCount) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  author: {
    type: Object,
    required: true
  }
})

defineEmits(['select'])

function getInitials(fullName) {
  if (!fullName) return '?'

  const parts = fullName.trim().split(' ')
  if (parts.length === 0) return '?'

  // Берём первую букву имени и первую букву фамилии
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase()
  }

  // Если только одно слово — берём первые 2 буквы
  return fullName.slice(0, 2).toUpperCase()
}

function pluralizeBooks(count) {
  const lastDigit = count % 10
  const lastTwoDigits = count % 100

  if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
    return 'книг'
  }

  if (lastDigit === 1) {
    return 'книга'
  }

  if (lastDigit >= 2 && lastDigit <= 4) {
    return 'книги'
  }

  return 'книг'
}
</script>

<style scoped>
.author-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background-color: #fff;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.author-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  transform: scaleY(0);
  transition: transform 0.3s;
}

.author-card:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  transform: translateY(-4px);
}

.author-card:hover::before {
  transform: scaleY(1);
}

.author-card:active {
  transform: translateY(-2px);
}

.author-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: transform 0.3s;
}

.author-card:hover .author-avatar {
  transform: scale(1.1) rotate(5deg);
}

.author-info {
  flex: 1;
  min-width: 0;
}

.author-name {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.book-count {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

@media (max-width: 768px) {
  .author-card {
    padding: 16px;
  }

  .author-avatar {
    width: 50px;
    height: 50px;
    font-size: 18px;
  }

  .author-name {
    font-size: 16px;
  }
}
</style>