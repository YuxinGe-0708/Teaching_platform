import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v2',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.response.use(
  res => res.data,
  err => {
    const msg = err.response?.data?.message || '请求失败'
    console.error(msg)
    return Promise.reject(err)
  }
)

export const authApi = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (username, password, role, name) => api.post('/auth/register', { username, password, role, name }),
  me: () => api.get('/auth/me'),
  logout: () => api.post('/auth/logout'),
  updateProfile: (data) => api.put('/auth/profile', data)
}

export const studentApi = {
  getCourses: () => api.get('/student/courses'),
  getAllCourses: () => api.get('/student/courses/all'),
  getCourseDetail: (id) => api.get(`/student/courses/${id}`),
  enroll: (inviteCode) => api.post('/student/courses/enroll', { inviteCode }),
  unenroll: (courseId) => api.post(`/student/courses/${courseId}/unenroll`),
  getTasks: () => api.get('/student/tasks'),
  getTaskDetail: (taskId) => api.get(`/student/tasks/${taskId}`),
  submitTask: (taskId, content) => api.post(`/student/tasks/${taskId}/submit`, { content }),
  getSubmission: (taskId) => api.get(`/student/tasks/${taskId}/submission`),
  getSubmissions: () => api.get('/student/submissions'),
  getProfile: () => api.get('/student/profile'),
}

export const judgeApi = {
  submit: (taskId, code, language) => api.post('/judge/submit', { taskId, code, language }),
  getResult: (submissionId) => api.get(`/judge/result/${submissionId}`)
}

export default api
