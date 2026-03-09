<!-- src/components/LoginRegisterView.vue -->
<template>
  <div class="wrapper">
    <div class="container" :class="{ active: isRegistering }">
      <!-- Логин форма -->
      <div class="form-box login">
        <form @submit.prevent="login">
          <h1>Login</h1>
          <div class="input-box">
            <input
              type="text"
              v-model="loginForm.username"
              placeholder="Username"
              required
            />
            <i class="bx bxs-user"></i>
          </div>
          <div class="input-box">
            <input
              type="password"
              v-model="loginForm.password"
              placeholder="Password"
              required
            />
            <i class="bx bxs-lock-alt"></i>
          </div>
          <div class="forgot-link">
            <a href="#">Forgot Password?</a>
          </div>
          <button type="submit" class="btn">Login</button>
        </form>
      </div>

      <!-- Регистрация форма -->
      <div class="form-box register">
        <form @submit.prevent="register">
          <h1>Registration</h1>
          <div class="input-box">
            <input
              type="text"
              v-model="registerForm.username"
              placeholder="Username"
              required
            />
            <i class="bx bxs-user"></i>
          </div>
          <div class="input-box">
            <input
              type="email"
              v-model="registerForm.email"
              placeholder="Email"
              required
            />
            <i class="bx bxs-envelope"></i>
          </div>
          <div class="input-box">
            <input
              type="password"
              v-model="registerForm.password"
              placeholder="Password"
              required
            />
            <i class="bx bxs-lock-alt"></i>
          </div>
          <button type="submit" class="btn">Register</button>
        </form>
      </div>

      <!-- Переключатель -->
      <div class="toggle-box">
        <div class="toggle-panel toggle-left">
          <h1>Hello, Welcome!</h1>
          <p>Don't have an account?</p>
          <button class="btn register-btn" @click="isRegistering = true">
            Register
          </button>
        </div>

        <div class="toggle-panel toggle-right">
          <h1>Welcome Back!</h1>
          <p>Already have an account?</p>
          <button class="btn login-btn" @click="isRegistering = false">
            Login
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "LoginRegisterView",
  data() {
    return {
      isRegistering: false,
      loginForm: { username: "", password: "" },
      registerForm: { username: "", email: "", password: "" },
    };
  },
  methods: {
    // В LoginRegisterView.vue или где у вас логин

    async login() {
      try {
        const response = await fetch("/api/auth/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include", // ← ВАЖНО для cookie
          body: JSON.stringify({
            username: this.loginForm.username,
            password: this.loginForm.password,
          }),
        });

        if (response.ok) {
          const data = await response.json();

          // Сохраняем токен в cookie вручную (если HttpOnly: false)
          // ИЛИ просто полагаемся на серверную cookie (если HttpOnly: true)
          document.cookie = `jwt=${data.token}; path=/; max-age=86400; SameSite=Lax`;

          this.$router.push("/");
        } else {
          alert("Неверный логин или пароль");
        }
      } catch (error) {
        alert("Ошибка подключения");
      }
    },

    async register() {
      try {
        const registrationData = {
          ...this.registerForm,
          roleId: 3,
        };

        const response = await this.$http.post("/users", registrationData);

        alert("Регистрация успешна! Теперь войдите в систему.");
        this.isRegistering = false;
        this.registerForm = {
          username: "",
          email: "",
          password: "",
        };
      } catch (error) {
        if (error.response?.status === 400) {
          alert(
            "Ошибка валидации: " +
              (error.response.data || "Проверьте введенные данные"),
          );
        } else if (error.response?.status === 409) {
          alert("Пользователь с таким именем или email уже существует");
        } else {
          alert(
            "Ошибка регистрации: " + (error.response?.data || error.message),
          );
        }
      }
    },
  },
};
</script>

<style scoped>
@import url("https://fonts.googleapis.com/css2?family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&display=swap");

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "Poppins", sans-serif;
  text-decoration: none;
  list-style: none;
}

.wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #fdf8ed;
  padding: 20px;
}

.container {
  position: relative;
  width: min(850px, 100%);
  height: 550px;
  background: #fff;
  border-radius: 30px;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.container h1 {
  font-size: 36px;
  margin: -10px 0;
  color: #5a4033;
}

.container p {
  font-size: 14.5px;
  margin: 15px 0;
  color: #6b5a50;
}

form {
  width: 100%;
}

.form-box {
  position: absolute;
  right: 0;
  width: 50%;
  height: 100%;
  background: #fff;
  display: flex;
  align-items: center;
  color: #333;
  text-align: center;
  padding: 40px;
  z-index: 1;
  transition:
    0.6s ease-in-out 1.2s,
    visibility 0s 1s;
}

.container.active .form-box {
  right: 50%;
}

.form-box.register {
  visibility: hidden;
}

.container.active .form-box.register {
  visibility: visible;
}

.input-box {
  position: relative;
  margin: 30px 0;
}

.input-box input {
  width: 100%;
  padding: 13px 50px 13px 20px;
  background: #eee;
  border-radius: 8px;
  border: none;
  outline: none;
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.input-box input::placeholder {
  color: #888;
  font-weight: 400;
}

.input-box i {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
}

.forgot-link {
  margin: -15px 0 15px;
}

.forgot-link a {
  font-size: 14.5px;
  color: #5a4033;
}

.btn {
  width: 100%;
  height: 48px;
  background: #e8d9c8;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.05);
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #5a4033;
  font-weight: 600;
  transition: background-color 0.2s;
}

.btn:hover {
  background: #f0e6d9;
}

.toggle-box {
  position: absolute;
  width: 100%;
  height: 100%;
}

.toggle-box::before {
  content: "";
  position: absolute;
  left: -250%;
  width: 300%;
  height: 100%;
  background: #5a4033;
  border-radius: 150px;
  z-index: 2;
  transition: 1.8s ease-in-out;
}

.container.active .toggle-box::before {
  left: 50%;
}

.toggle-panel {
  position: absolute;
  width: 50%;
  height: 100%;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 2;
  transition: 0.6s ease-in-out;
}

.toggle-panel.toggle-left {
  left: 0;
  transition-delay: 1.2s;
}

.container.active .toggle-panel.toggle-left {
  left: -50%;
  transition-delay: 0.6s;
}

.toggle-panel.toggle-right {
  right: -50%;
  transition-delay: 0.6s;
}

.container.active .toggle-panel.toggle-right {
  right: 0;
  transition-delay: 1.2s;
}

.toggle-panel p {
  margin-bottom: 20px;
  color: #fff;
}

.toggle-panel .btn {
  width: 160px;
  height: 46px;
  background: transparent;
  border: 2px solid #fff;
  box-shadow: none;
  color: #fff;
}

.toggle-panel .btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

@media screen and (max-width: 650px) {
  .container {
    height: calc(100vh - 40px);
  }

  .form-box {
    bottom: 0;
    width: 100%;
    height: 70%;
  }

  .container.active .form-box {
    right: 0;
    bottom: 30%;
  }

  .toggle-box::before {
    left: 0;
    top: -270%;
    width: 100%;
    height: 300%;
    border-radius: 20vw;
  }

  .container.active .toggle-box::before {
    left: 0;
    top: 70%;
  }

  .container.active .toggle-panel.toggle-left {
    left: 0;
    top: -30%;
  }

  .toggle-panel {
    width: 100%;
    height: 30%;
  }

  .toggle-panel.toggle-left {
    top: 0;
  }

  .toggle-panel.toggle-right {
    right: 0;
    bottom: -30%;
  }

  .container.active .toggle-panel.toggle-right {
    bottom: 0;
  }
}

@media screen and (max-width: 400px) {
  .form-box {
    padding: 20px;
  }

  .toggle-panel h1 {
    font-size: 30px;
  }
}
</style>
