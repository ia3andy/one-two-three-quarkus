import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { gameApi, runApi, sensors } from '../../api';
import Generator from './Generator';
import { ENABLE_SHAKING } from '../../Config';
import TopBar from './TopBar';
import EnableShakingModal from './EnableShakingModal';
import RankModal from './RankModal';
import logo from '../../assets/logo.svg';

const Container = styled.div`
  text-align: center;
  color: white;
  font-size: 1rem;
  display: flex;
  align-items: center;
  top: 70px;
  bottom: 00px;
  width: 100%;
  position: fixed;
  justify-content: center;
  flex-direction: column;
`;

const EndDiv = styled.div`
  display: flex;
  flex-direction: column;
  font-size: 2rem;

  img {
    height: 120px;
  }
`;

const LoadingDiv = styled.div`
  display: flex;
  flex-direction: column;
  font-size: 2rem;

  img {
    height: 120px;
    animation: spin 3s linear infinite;
  }

  & > div:first-child {
    flex-basis: 150px;
  }

  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
`;

function Main(props) {
  switch (props.state.status) {
    case 'alive':
      return (
        <Generator
          run={props.run}
          distance={props.distance}
          shakingEnabled={props.shakingEnabled}
        />
      );
    case 'dead':
      return (
          <EndDiv>
            RIP
          </EndDiv>
      );
    case 'saved':
      return (
          <EndDiv>
            SAVED
          </EndDiv>
      );
    case 'completed':
      return (
        <RankModal data={props.state.data} user={props.user} />
      );
    default:
      return (
        <LoadingDiv>
          <div><img src={`/static/bundle/${logo}`} alt="logo" /></div>
          <div>Waiting for game...</div>
        </LoadingDiv>
      );
  }
}

export default function GameController() {
  const [user, setUser] = useState();
  const [state, setState] = useState({ status: 'offline' });
  const [distance, setDistance] = useState(0);
  const [pingTimeout, setPingTimeout] = useState();
  const [shakingEnabled, setShakingEnabled] = useState(false);

  function reset() {
    setDistance(0);
  }

  function run(distance) {
    if (user && (distance === 0 || state.status === 'alive')) {
      console.log(`Run: ${distance}`);
      clearTimeout(pingTimeout);
      setPingTimeout(null);
      setDistance((c) => c + distance);
      runApi.run(user, distance).then(() => {
      });
    }
  }

  function enableShaking(e) {
    e.preventDefault();
    sensors.enableShakeSensor();
    setShakingEnabled(true);
  }

  useEffect(() => {
    gameApi.assign().then(setUser);
  }, []);
  useEffect(() => {
    if (user && !pingTimeout) {
      setPingTimeout((p) => (!p ? setTimeout(() => run(0), 3000) : null));
    }
    return () => clearTimeout(pingTimeout);
  }, [user, pingTimeout]);
  useEffect(() => gameApi.events(user, setState, reset), [user]);
  return (
    <Container>
      {user && (
        <>
          <TopBar user={user} status={state.status}  />
          <Main
            user={user}
            state={state}
            run={run}
            distance={distance}
            shakingEnabled={shakingEnabled}
          />
          {ENABLE_SHAKING && !shakingEnabled
                    && <EnableShakingModal onClick={enableShaking} />}
        </>
      )}
      {!user &&
        <>Game not available yet, reload..</>
      }
    </Container>
  );
}
