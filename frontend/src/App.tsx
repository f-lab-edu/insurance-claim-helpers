import { Routes, Route } from 'react-router-dom';
import HomePage from './routes/HomePage';
import LoginPage from './routes/LoginPage';
import ChatPage from './routes/ChatPage';
import MyPage from './routes/MyPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/chat/:sessionId" element={<ChatPage />} />
      <Route path="/mypage" element={<MyPage />} />
    </Routes>
  );
}