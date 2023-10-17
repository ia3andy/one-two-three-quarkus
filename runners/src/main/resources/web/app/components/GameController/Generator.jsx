import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import {
  TAP_DISTANCE, ENABLE_TAPPING, ENABLE_SWIPING,
} from '../../Config';
import { sensors } from '../../api';
import { utils } from "../../api";
import "./Generator.scss"


const DistanceIndicatorDiv = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  font-size: 3rem;
  font-weight: bold;
  line-height: 3rem;
  margin-top: 40px;

  &:after {
    content: '${(props) => props.unit}';
    font-size: 1.5rem;
    line-height: 1.5rem;
    font-weight: bold;
  }
`;

function Generator(props) {
  const [running, setRunning] = useState(0);
  const [runningTimeout, setRunningTimeout] = useState(-1);

  const run = () =>  {
    setRunning(1);
    setRunningTimeout(p => {
      if(p > 0) {
        clearTimeout(p);
      }
      return setTimeout(() => setRunning(0), 300);
    });
  }

  // Swipe
  if (ENABLE_SWIPING) {
    useEffect(() => {
      sensors.startSwipeSensor((d, diff) => {
        const power = Math.min(100, Math.round(Math.abs(diff) / 5));
        run();
        props.run(power);
      });
    }, []);
  }

  // Shake
  if (props.shakingEnabled) {
    useEffect(() => {
      sensors.startShakeSensor((magnitude) => {
        const power = Math.round(Math.min(magnitude, 100) * 3 / 100);
        run();
        props.run(power);
      });
    }, []);
  }

  const onTap = (e) => {
    // Tapping
    if (ENABLE_TAPPING) {
      run();
      props.run(TAP_DISTANCE, true);
    }
  };

  const onClick = (e) => {
    // Clicking
    if (!utils.IS_TOUCH_DEVICE && ENABLE_TAPPING) {
      run();
      props.run(TAP_DISTANCE, true);
    }
  };

  return (
    <>
        <div className="running" onClick={onClick} onTouchStart={onTap} running={running}>
          <div className="outer">
            <div className="body">
              <div className="arm behind"></div>
              <div className="arm front"></div>
              <div className="leg behind"></div>
              <div className="leg front"></div>
            </div>
          </div>
        </div>

      <DistanceIndicatorDiv
        unit="m"
        id="distance-indicator"
      >
        {props.distance}
      </DistanceIndicatorDiv>
    </>
  );
}

export default Generator;
