import React from 'react';
import styled from 'styled-components';

const RankModalOverlay = styled.div`
  position: fixed;
  background-color: #4695EB;
  top: 70px;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(3px);
  align-items: center;
  justify-content: center;

  h1 {
    font-size: 2rem;
    text-transform: uppercase;
  }

  .rank {
    font-size: 6rem;
    font-weight: bold;
  }
`;

export default function RankModal(props) {
    return (
        <RankModalOverlay>
            <h1>{props.text}</h1>
            {props.rank && (
                <>
                    <p>You ranked: </p>
                    <div className="rank">{props.rank}</div>
                </>
            )}
        </RankModalOverlay>
    );
}
