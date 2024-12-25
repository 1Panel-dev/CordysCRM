<template>
  <div class="login-container">
    <h2>Login</h2>
    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label for="username">Username</label>
        <input
            v-model="username"
            id="username"
            type="text"
            placeholder="Enter your username"
            :class="{'input-error': errors.username}"
        />
        <span v-if="errors.username" class="error-message">{{ errors.username }}</span>
      </div>

      <div class="form-group">
        <label for="password">Password</label>
        <input
            v-model="password"
            id="password"
            type="password"
            placeholder="Enter your password"
            :class="{'input-error': errors.password}"
        />
        <span v-if="errors.password" class="error-message">{{ errors.password }}</span>
      </div>

      <button type="submit" :disabled="isSubmitting">Login</button>
    </form>
  </div>
</template>

<script>
import { ref } from 'vue';

export default {
  name: 'Login',
  setup() {
    const username = ref('');
    const password = ref('');
    const errors = ref({
      username: '',
      password: '',
    });
    const isSubmitting = ref(false);

    const validate = () => {
      errors.value.username = '';
      errors.value.password = '';

      if (!username.value) {
        errors.value.username = 'Username is required.';
      }

      if (!password.value) {
        errors.value.password = 'Password is required.';
      }

      return !errors.value.username && !errors.value.password;
    };

    const handleLogin = () => {
      if (validate()) {
        isSubmitting.value = true;
        // Simulate login request (you can replace this with an actual API call)
        setTimeout(() => {
          alert('Login successful');
          isSubmitting.value = false;
        }, 1000);
      }
    };

    return {
      username,
      password,
      errors,
      isSubmitting,
      handleLogin,
    };
  },
};
</script>

<style scoped>
.login-container {
  max-width: 400px;
  margin: 0 auto;
  padding: 2rem;
  border: 1px solid #ccc;
  border-radius: 8px;
  background-color: #f9f9f9;
}

h2 {
  text-align: center;
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

input {
  width: 100%;
  padding: 0.75rem;
  margin-top: 0.25rem;
  border-radius: 4px;
  border: 1px solid #ccc;
}

.input-error {
  border-color: red;
}

.error-message {
  color: red;
  font-size: 0.875rem;
}

button {
  width: 100%;
  padding: 0.75rem;
  border: none;
  background-color: #4CAF50;
  color: white;
  font-size: 1rem;
  border-radius: 4px;
  cursor: pointer;
}

button:disabled {
  background-color: #ccc;
}
</style>
