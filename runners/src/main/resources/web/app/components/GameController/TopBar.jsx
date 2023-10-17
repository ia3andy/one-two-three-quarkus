import React from 'react';
import { HeartPulse, CloudSlash, CloudCheck } from 'react-bootstrap-icons';
import styled from 'styled-components';

const TopBarDiv = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 70px;
  display: flex;
  color: white;
  font-size: 2rem;
  background-color: var(--top-bar);
  align-items: center;
  padding-right: 10px;
  padding-left: 10px;

  div {
    text-align: left;
  }
`;

const UserDiv = styled.div`
  flex-grow: 1;
  font-size: 2rem;
`;

const StatusDiv = styled.div`
  margin-left: 10px;
  color: #FAC898;
`;

function TopBar(props) {
  const statusColor = props.status !== 'offline' ? 'green' : 'grey';
  return (
    <TopBarDiv>
      <UserDiv>
        {props.user.name}
      </UserDiv>
      <StatusDiv color={statusColor}>
        {props.status === 'alive' && <HeartPulse size={32} />}
        {props.status === 'offline' && <CloudSlash size={32} />}
        {props.status === 'off' && <CloudCheck size={32} />}
      </StatusDiv>
    </TopBarDiv>
  );
}

export default TopBar;
