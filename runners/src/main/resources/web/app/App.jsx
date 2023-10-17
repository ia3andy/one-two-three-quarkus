import React from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import GameController from './components/GameController';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<GameController />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
